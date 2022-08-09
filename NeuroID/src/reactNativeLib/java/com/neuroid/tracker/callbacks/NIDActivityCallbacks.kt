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

class NIDActivityCallbacks() : ActivityLifecycleCallbacks {
    private var auxOrientation = -1
    private var activitiesStarted = 1
    private var listActivities = ArrayList<String>()
    private var wasChanged = false

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        val existActivity = listActivities.contains(currentActivityName)

        NIDServiceTracker.screenActivityName = currentActivityName
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"

        val changedOrientation = auxOrientation != orientation
        wasChanged = changedOrientation

        if (existActivity.not()) {
            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
            fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)
        }

        if (changedOrientation) {
            val strOrientation = if (auxOrientation == 1) {
                "Landscape"
            } else {
                "Portrait"
            }

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_ORIENTATION_CHANGE,
                        ts = System.currentTimeMillis(),
                        tg = hashMapOf(
                            "orientation" to strOrientation
                        )
                    )
                )
            auxOrientation = orientation
        }

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis()
                )
            )
    }

    override fun onActivityStarted(activity: Activity) {
        var cameBackFromBehind = false
        if (activitiesStarted == 0) {
            cameBackFromBehind = true
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_FOCUS,
                        ts = System.currentTimeMillis()
                    )
                )
        }
        activitiesStarted++

        val currentActivityName = activity::class.java.name
        val existActivity = listActivities.contains(currentActivityName)

        val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
        val hasFragments = fragManager?.hasFragments() ?: false

        if (existActivity) {
            if (hasFragments.not() && cameBackFromBehind.not()) {
                registerTargetFromScreen(activity, registerTarget = true, registerListeners = false)
            }
        } else {
            listActivities.add(currentActivityName)
            if (hasFragments.not()) {
                registerTargetFromScreen(activity, wasChanged.not(), true)
            }
            wasChanged = false
            registerWindowListeners(activity)
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
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_BLUR,
                        ts = System.currentTimeMillis()
                    )
                )
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        val currentActivityName = activity::class.java.name
        listActivities.remove(currentActivityName)
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis()
                )
            )
    }
}