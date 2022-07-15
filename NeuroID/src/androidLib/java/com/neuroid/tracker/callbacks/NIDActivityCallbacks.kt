package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.hasFragments

class NIDActivityCallbacks(private val nidDataStoreManager: NIDDataStoreManager) :
    ActivityLifecycleCallbacks {
    private var auxOrientation = -1
    private var activitiesStarted = 0
    private var listActivities = ArrayList<String>()
    private var wasChanged = false

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        if (auxOrientation == -1) {
            auxOrientation = orientation
        }
        val existActivity = listActivities.contains(currentActivityName)

        NIDServiceTracker.screenActivityName = currentActivityName
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"

        wasChanged = auxOrientation != orientation

        if (existActivity.not()) {
            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
            fragManager?.registerFragmentLifecycleCallbacks(
                NIDFragmentCallbacks(
                    nidDataStoreManager,
                    wasChanged
                ), true
            )
        }

        if (wasChanged) {
            val strOrientation = if (auxOrientation == 1) {
                "Landscape"
            } else {
                "Portrait"
            }

            nidDataStoreManager
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

        nidDataStoreManager
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
            nidDataStoreManager
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
                registerTargetFromScreen(
                    nidDataStoreManager,
                    activity,
                    registerTarget = true,
                    registerListeners = false
                )
            }
        } else {
            listActivities.add(currentActivityName)
            if (hasFragments.not()) {
                registerTargetFromScreen(nidDataStoreManager, activity, wasChanged.not())
            }
            wasChanged = false
            registerWindowListeners(nidDataStoreManager, activity)
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
            nidDataStoreManager
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
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)
        nidDataStoreManager
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis()
                )
            )
    }
}