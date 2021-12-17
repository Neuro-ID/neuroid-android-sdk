package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var actualOrientation = 0

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        val orientation = activity.resources.configuration.orientation

        (activity as? AppCompatActivity)?.supportFragmentManager
            ?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)
        registerViewsEventsForActivity(activity)

        if (actualOrientation == 0) {
            actualOrientation = orientation
        } else if (actualOrientation != orientation) {
            val strOrientation = if (actualOrientation == 1) {
                "Landscape"
            } else {
                "Portrait"
            }

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_ORIENTATION_CHANGE,
                    ts = System.currentTimeMillis(),
                    tgs = hashMapOf(
                        "orientation" to strOrientation
                    )
                ).getOwnJson())
            actualOrientation = orientation
        }
    }

    override fun onActivityStarted(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_LOAD,
                et = "ACTIVITY",
                ts = System.currentTimeMillis()
            ).getOwnJson())
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_FOCUS,
                et = "ACTIVITY",
                ts = System.currentTimeMillis()
            ).getOwnJson())
    }

    override fun onActivityPaused(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_BLUR,
                et = "ACTIVITY",
                ts = System.currentTimeMillis()
            ).getOwnJson())
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        // No Operation
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_UNLOAD,
                et = "ACTIVITY",
                ts = System.currentTimeMillis()
            ).getOwnJson())
        unRegisterListenerFromActivity(activity)
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityDestroyed")
    }
}