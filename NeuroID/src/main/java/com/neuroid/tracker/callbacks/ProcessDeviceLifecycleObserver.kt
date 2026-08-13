package com.neuroid.tracker.callbacks

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.utils.NIDLog

/**
 *
 * This is registered against [androidx.lifecycle.ProcessLifecycleOwner], **not** a per-Activity
 * `Application.ActivityLifecycleCallbacks`. `ProcessLifecycleOwner`'s `onStart` only fires when
 * the first `Activity` in the process reaches `onStart()` - i.e. true foreground entry - so it
 * will *not* fire during headless process wake-ups (e.g. a `BroadcastReceiver`-triggered FCM/push
 * cold start) where no `Activity` is ever created.
 */
internal class ProcessDeviceLifecycleObserver(
    private val neuroID: NeuroID,
) : DefaultLifecycleObserver {

    var started: Boolean = false

    override fun onStart(owner: LifecycleOwner) {
        if (started) {
            return
        }

        neuroID.captureEvent(queuedEvent = true, type = LOG, m = "isAdvancedDevice setting (onStart): ${neuroID.isAdvancedDevice}", level = "INFO")

        if (neuroID.isAdvancedDevice) {
            neuroID.checkThenCaptureAdvancedDevice()
        }

        neuroID.captureApplicationMetaData()
        neuroID.configService.retrieveOrRefreshCache(neuroID)

        started = true
    }
}
