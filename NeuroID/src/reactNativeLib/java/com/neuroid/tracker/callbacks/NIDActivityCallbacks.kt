package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.neuroid.tracker.events.WINDOW_ORIENTATION_CHANGE
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.unRegisterListenerFromActivity
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var auxOrientation = 0
    private var activitiesStarted = 1
    private var previousActivityName = ""

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
       //No op
    }

    override fun onActivityStarted(activity: Activity) {
        if (activitiesStarted == 0) {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_FOCUS,
                    ts = System.currentTimeMillis()
                ))
        }
        activitiesStarted++

        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        if (auxOrientation == 0) { auxOrientation = orientation }

        if (previousActivityName == currentActivityName) {
            if (auxOrientation != orientation) {
                val strOrientation = if (auxOrientation == 1) {
                    "Landscape"
                } else {
                    "Portrait"
                }

                getDataStoreInstance()
                    .saveEvent(NIDEventModel(
                        type = WINDOW_ORIENTATION_CHANGE,
                        ts = System.currentTimeMillis(),
                        tg = hashMapOf(
                            "orientation" to strOrientation
                        )
                    ))
                auxOrientation = orientation
            }
        } else {
            previousActivityName = currentActivityName
        }
    }

    override fun onActivityResumed(activity: Activity) {
        //No operation
    }

    override fun onActivityPaused(activity: Activity) {
        //No operation
    }

    override fun onActivityStopped(activity: Activity) {
        activitiesStarted--
        if (activitiesStarted == 0) {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_BLUR,
                    ts = System.currentTimeMillis()
                ))
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        unRegisterListenerFromActivity(activity)
    }
}