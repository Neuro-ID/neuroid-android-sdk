package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.hasFragments

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var auxOrientation = 0
    private var activitiesStarted = 0
    private var previousActivityName = ""

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        if (auxOrientation == 0) { auxOrientation = orientation }

        NIDServiceTracker.screenActivityName = currentActivityName
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"

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

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis()
                ))

            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
            val hasFragments = fragManager?.hasFragments() ?: false

            registerViewsEventsForActivity(activity, hasFragments.not())
            if (hasFragments) {
                fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)
            }
        }
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
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_UNLOAD,
                ts = System.currentTimeMillis()
            ))
        unRegisterListenerFromActivity(activity)
    }
}