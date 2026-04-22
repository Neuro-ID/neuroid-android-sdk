package com.neuroid.tracker.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDSdkVersionProvider
import java.util.function.Consumer

class NIDScreenCaptureService(
    private val logger: NIDLogWrapper,
    private val sdkVersionProvider: NIDSdkVersionProvider = NIDSdkVersionProvider(),
) {
    companion object {
        private const val TAG = "NIDScreenCaptureService"
    }

    // References kept for cleanup
    private var screenCaptureCallback: Any? = null // typed as Any to avoid class reference on < API 34
    private var screenRecordingCallback: Any? = null // typed as Any to avoid class reference on < API 35
    private var registeredActivity: Activity? = null

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
                val currentState = activity.windowManager.addScreenRecordingCallback(
                    activity.mainExecutor,
                    recordingConsumer,
                )
                // Notify listener immediately if recording is already in progress at registration time
                if (currentState == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE) {
                    logger.d(TAG, "Screen recording already in progress at registration time")
                    listenerRecording.onScreenRecorded(true)
                }
            } else {
                logger.d(TAG, "DETECT_SCREEN_RECORDING permission not granted")
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
                logger.d(TAG, "DETECT_SCREEN_CAPTURE permission not granted")
            }
        }
    }

    /**
     * Removes the screen capture listeners only if [activity] is the one currently registered.
     * Safe to call even if no listener was registered or if a different activity owns the registration.
     */
    @SuppressLint("MissingPermission", "NewApi")
    fun teardownScreenCaptureListener(activity: Activity) {
        if (registeredActivity !== activity) {
            logger.d(TAG, "teardown skipped — activity is not the registered owner")
            return
        }
        teardownScreenCaptureListener()
    }

    /**
     * Removes the screen capture listeners that were previously registered.
     * Safe to call even if no listener was registered.
     */
    @SuppressLint("MissingPermission", "NewApi")
    fun teardownScreenCaptureListener() {
        teardownScreenCaptureCallback()
        tearDownRecordingCallback()
        screenCaptureCallback = null
        screenRecordingCallback = null
        registeredActivity = null
    }

    private fun teardownScreenCaptureCallback() {
        val activity = registeredActivity
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
    }

    private fun tearDownRecordingCallback() {
        val activity = registeredActivity
        if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val callback = screenRecordingCallback
                if (callback != null && activity != null && ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.DETECT_SCREEN_RECORDING,
                    ) == PackageManager.PERMISSION_GRANTED) {

                        @Suppress("UNCHECKED_CAST")
                        activity.windowManager.removeScreenRecordingCallback(
                            callback as Consumer<Int>,
                        )
                        logger.d(TAG, "Screen recording callback unregistered")
                    } else {
                        logger.d(
                            TAG,
                            "DETECT_SCREEN_RECORDING permission not granted or some error has occurred, skipping unregister"
                        )
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error unregistering screen recording callback: ${e.message}")
            }
        }
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