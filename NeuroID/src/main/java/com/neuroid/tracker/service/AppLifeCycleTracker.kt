package com.neuroid.tracker.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.NeuroID

class AppLifecycleTracker : DefaultLifecycleObserver {
    var isAppInForeground: Boolean = false
        private set

    override fun onCreate(owner: LifecycleOwner) {
        Log.d("SDK AppLifecycleTracker", "PLO onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        NeuroID.getInternalInstance().let { nid ->
            Log.d("SDK AppLifecycleTracker", "NID CheckADV")
            nid!!.checkThenCaptureAdvancedDevice(true)
            nid.resumeCollection()
        }
        Log.d("SDK AppLifecycleTracker", "PLO onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.d("SDK AppLifecycleTracker", "PLO onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        // Fires with a delay to survive rotation of false positives
        Log.d("SDK AppLifecycleTracker", "PLO onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        // Fires with a delay to survive rotation of false positives
        isAppInForeground = false
        NeuroID.getInternalInstance()?.pauseCollection()
        Log.d("SDK AppLifecycleTracker", "PLO onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // ProcessLifecycleOwner never delivers this — included for completeness
        Log.d("SDK AppLifecycleTracker", "PLO onDestroy")
    }
}