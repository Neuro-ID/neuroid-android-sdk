package com.neuroid.tracker.callbacks

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.NeuroID

/**
 * Triggers the advanced-device capture exactly once, the first time the host
 * application's process actually reaches the foreground.
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
    override fun onStart(owner: LifecycleOwner) {
        neuroID.checkThenCaptureAdvancedDevice()
    }
}
