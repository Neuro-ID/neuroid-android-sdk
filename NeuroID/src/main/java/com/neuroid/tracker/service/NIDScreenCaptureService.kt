package com.neuroid.tracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDSdkVersionProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.Consumer

class NIDScreenCaptureService(
    private val logger: NIDLogWrapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sdkVersionProvider: NIDSdkVersionProvider = NIDSdkVersionProvider(),
) {
    companion object {
        private const val TAG = "NIDScreenCaptureService"

        // Common screenshot directory names across Android OEMs
        private val SCREENSHOT_PATH_KEYWORDS = listOf(
            "screenshot",
            "screenshots",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "screencapture",
            "screen_capture",
            "screen-capture",
            "screen capture",
        )
    }

    // References kept for cleanup
    private var contentObserver: ContentObserver? = null
    private var screenCaptureCallback: Any? = null // typed as Any to avoid class reference on < API 34
    private var screenRecordingCallback: Any? = null // typed as Any to avoid class reference on < API 35
    private var registeredActivity: Activity? = null
    private var coroutineScope: CoroutineScope? = null

    private var captureUriPath: String = ""

    /**
     * Sets up a screen capture (screenshot) listener on the provided Activity.
     *
     * - API 34+ (Android 14+): Uses the native Activity.ScreenCaptureCallback API.
     * - API 24–33: Uses a ContentObserver monitoring MediaStore for new images whose
     *   file path contains a screenshot-related keyword.
     */
    @SuppressLint("MissingPermission", "InlinedApi")
    fun setupScreenCaptureListener(activity: Activity, listener: ScreenCaptureListener, listenerRecording: ScreenRecordingListener) {
        // Clean up any previous registration
        teardownScreenCaptureListener()

        registeredActivity = activity

        // setup screen recording callback for API 35+
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            logger.d(TAG, "Setting up native ScreenCapture (API ${sdkVersionProvider.getSdkInt()})")
            val hasPermission = ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.DETECT_SCREEN_RECORDING,
                ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                val recordingConsumer = Consumer<Int> { state ->
                    val isRecording = state == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
                    logger.d(TAG, "Screen recording state changed: isRecording=$isRecording")
                    listenerRecording.onScreenRecorded(isRecording)
                }
                screenRecordingCallback = recordingConsumer
                activity.windowManager.addScreenRecordingCallback(
                    activity.mainExecutor,
                    recordingConsumer,
                )
            } else {
                logger.d(TAG, "DETECT_SCREEN_RECORDING permission not granted, skipping screen recording callback setup")
            }
        }
        // setup screen capture callback for API 34+
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ — native screenshot detection
            val hasPermission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.DETECT_SCREEN_CAPTURE,
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                setupNativeScreenCaptureCallback(activity, listener)
            } else {
                logger.d(TAG, "DETECT_SCREEN_CAPTURE permission not granted, skipping native setup")
            }
        } else {
            // API 24-33 — ContentObserver fallback
            setupContentObserverFallback(activity, listener)
        }

        logger.d(TAG, "Screen capture listener registered (API ${sdkVersionProvider.getSdkInt()})")
    }

    /**
     * Removes the screen capture listener that was previously registered.
     * Safe to call even if no listener was registered.
     */
    @SuppressLint("MissingPermission", "NewApi")
    fun teardownScreenCaptureListener() {
        val activity = registeredActivity

        // Unregister screen recording callback (API 35+)
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val callback = screenRecordingCallback
                if (callback != null && activity != null) {
                    val hasPermission = ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.DETECT_SCREEN_RECORDING,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        @Suppress("UNCHECKED_CAST")
                        activity.windowManager.removeScreenRecordingCallback(
                            callback as Consumer<Int>,
                        )
                        logger.d(TAG, "Screen recording callback unregistered")
                    } else {
                        logger.d(TAG, "DETECT_SCREEN_RECORDING permission not granted, skipping unregister")
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error unregistering screen recording callback: ${e.message}")
            }
        }

        // Unregister native callback (API 34+)
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val callback = screenCaptureCallback
                if (callback != null && activity != null) {
                    val hasPermission = ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.DETECT_SCREEN_CAPTURE,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        activity.unregisterScreenCaptureCallback(
                            callback as Activity.ScreenCaptureCallback,
                        )
                        logger.d(TAG, "Native ScreenCaptureCallback unregistered")
                    } else {
                        logger.d(TAG, "DETECT_SCREEN_CAPTURE permission not granted, skipping unregister")
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error unregistering native ScreenCaptureCallback: ${e.message}")
            }
        }

        // Unregister ContentObserver fallback
        try {
            val observer = contentObserver
            if (observer != null && activity != null) {
                activity.contentResolver.unregisterContentObserver(observer)
                logger.d(TAG, "ContentObserver unregistered")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error unregistering ContentObserver: ${e.message}")
        }

        screenCaptureCallback = null
        screenRecordingCallback = null
        contentObserver = null
        registeredActivity = null

        coroutineScope?.cancel()
        coroutineScope = null
    }

    // -----------------------------------------------------------------------
    // API 34+ — Native ScreenCaptureCallback
    // -----------------------------------------------------------------------


    @RequiresPermission(Manifest.permission.DETECT_SCREEN_CAPTURE)
    @Suppress("NewApi")
    private fun setupNativeScreenCaptureCallback(
        activity: Activity,
        listenerScreen: ScreenCaptureListener,
    ) {
        val callback = Activity.ScreenCaptureCallback {
            logger.d(TAG, "Screenshot detected via native ScreenCaptureCallback")
            listenerScreen.onScreenCaptured()
        }

        screenCaptureCallback = callback
        activity.registerScreenCaptureCallback(
            activity.mainExecutor,
            callback,
        )
    }

    // -----------------------------------------------------------------------
    // API 24-33 — ContentObserver fallback
    // -----------------------------------------------------------------------

    private fun setupContentObserverFallback(
        activity: Activity,
        listenerScreen: ScreenCaptureListener,
    ) {
        val handler = Handler(Looper.getMainLooper())
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        coroutineScope = scope

        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                if (uri == null || captureUriPath.equals(uri.toString())) return
                captureUriPath = uri.toString()

                // Run the detection on a background thread to
                // avoid StrictMode DiskReadViolation on the main thread.
                scope.launch {
                    try {
                        val detected = isScreenshotUri(activity, uri)
                        if (detected) {
                            logger.d(TAG, "Screenshot detected via ContentObserver: $uri")
                            withContext(Dispatchers.Main) {
                                listenerScreen.onScreenCaptured()
                            }
                        }
                    } catch (e: Exception) {
                        logger.e(TAG, "Error processing ContentObserver change: ${e.message}")
                    }
                }
            }
        }

        contentObserver = observer

        activity.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
    }

    /**
     * Determines whether the given media content URI represents a screenshot.
     *
     * Uses a multi-strategy approach, requires READ_MEDIA_IMAGES permissions:
     *  1. Check the URI path string itself for screenshot keywords.
     *  2. Query DISPLAY_NAME (filename)
     *  3. Query RELATIVE_PATH (API 29+)
     *  4. Fallback to DATA column on API < 29 w
     */
    @Suppress("deprecation")
    internal fun isScreenshotUri(activity: Activity, uri: Uri): Boolean {
        // Strategy 1: Check the URI string itself
        if (isScreenshotPath(uri.toString())) {
            return true
        }

        // Strategy 2: Query DISPLAY_NAME (does not require storage permissions)
        try {
            val displayNameProjection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            activity.contentResolver.query(uri, displayNameProjection, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex =
                            cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        if (colIndex >= 0) {
                            val displayName = cursor.getString(colIndex)
                            if (displayName != null && isScreenshotPath(displayName)) {
                                return true
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            logger.e(TAG, "Error querying DISPLAY_NAME: ${e.message}")
        }

        // Strategy 3: Query RELATIVE_PATH (API 29+, no storage permission needed)
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.Q) {
            try {
                val relPathProjection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
                activity.contentResolver.query(uri, relPathProjection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val colIndex =
                                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                            if (colIndex >= 0) {
                                val relativePath = cursor.getString(colIndex)
                                if (relativePath != null && isScreenshotPath(relativePath)) {
                                    return true
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                logger.e(TAG, "Error querying RELATIVE_PATH: ${e.message}")
            }
        }

        // Strategy 4: Fallback to DATA column on API < 29 (works without permissions)
        if (sdkVersionProvider.getSdkInt() < Build.VERSION_CODES.Q) {
            try {
                val dataProjection = arrayOf(MediaStore.Images.Media.DATA)
                activity.contentResolver.query(uri, dataProjection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val colIndex =
                                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            if (colIndex >= 0) {
                                val filePath = cursor.getString(colIndex)
                                if (filePath != null && isScreenshotPath(filePath)) {
                                    return true
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                logger.e(TAG, "Error querying DATA column: ${e.message}")
            }
        }

        return false
    }

    /**
     * Checks whether the given file path looks like a screenshot based on
     * common directory/file naming conventions across Android OEMs.
     */
    internal fun isScreenshotPath(path: String): Boolean {
        val lowerPath = path.lowercase()
        return SCREENSHOT_PATH_KEYWORDS.any { keyword -> lowerPath.contains(keyword) }
    }

    interface ScreenCaptureListener {
        fun onScreenCaptured()
    }

    interface ScreenRecordingListener {
        fun onScreenRecorded(isRecording: Boolean)
    }

}