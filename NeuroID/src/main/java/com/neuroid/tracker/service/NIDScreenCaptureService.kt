package com.neuroid.tracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDSdkVersionProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
            // API 24 <= i < 33 not supported
            logger.d(TAG, "DETECT_SCREEN_CAPTURE not supported for API < 33 skipping native setup")
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

    interface ScreenCaptureListener {
        fun onScreenCaptured()
    }

    interface ScreenRecordingListener {
        fun onScreenRecorded(isRecording: Boolean)
    }

}