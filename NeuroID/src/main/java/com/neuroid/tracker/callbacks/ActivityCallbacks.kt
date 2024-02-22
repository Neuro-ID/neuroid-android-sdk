package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_ORIENTATION_CHANGE
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.registrationHelpers

class ActivityCallbacks(
    val dataStore: NIDDataStoreManager,
    val logger:NIDLogWrapper,
    val registrationHelper: RegistrationIdentificationHelper
): ActivityLifecycleCallbacks {
    private var activitiesStarted = 0
    private var listActivities = ArrayList<String>()

    private var auxOrientation = -1
    private var wasChanged = false

    /**
     * Option for customers to force start with Activity
     */
    fun forceStart(activity: Activity) {
        registrationHelper.registerTargetFromScreen(
            activity,
            true,
            true,
            activityOrFragment = "activity",
            parent = activity::class.java.simpleName
        )
        // register listeners for focus, blur and touch events
        registrationHelper.registerWindowListeners(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        NIDLog.d(msg = "onActivityCreated");
    }

     override fun onActivityStarted(activity: Activity) {
        NIDLog.d( msg="Activity - Created")

        val currentActivityName = activity::class.java.name

        val orientation = activity.resources.configuration.orientation
        if (auxOrientation == -1) {
            auxOrientation = orientation
        }
        val existActivity = listActivities.contains(currentActivityName)

        NeuroID.screenActivityName = currentActivityName
        if (NeuroID.firstScreenName.isNullOrEmpty()) {
            NeuroID.firstScreenName = currentActivityName
        }
        if (NeuroID.screenFragName.isNullOrEmpty()) {
            NeuroID.screenFragName = ""
        }
        if (NeuroID.screenName.isNullOrEmpty()) {
            NeuroID.screenName = "AppInit"
        }
        wasChanged = auxOrientation != orientation

        if (existActivity.not()) {
            NIDLog.d(msg="onActivityStarted existActivity.not()");

            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager

            NIDLog.d( msg="Activity - POST Created - REGISTER FRAGMENT LIFECYCLES")
            fragManager?.registerFragmentLifecycleCallbacks(
                FragmentCallbacks(
                    wasChanged,
                    dataStore,
                    logger,
                    registrationHelper
                ),
                true
            )
        }

        if (wasChanged) {
            NIDLog.d( msg="Activity - POST Created - Orientation change")
            dataStore.saveEvent(
                NIDEventModel(
                    type = WINDOW_ORIENTATION_CHANGE,
                    o = "CHANGED",
                )
            )
            auxOrientation = orientation
        }

        NIDLog.d(msg="Activity - POST Created - Window Load")
         dataStore.saveEvent(
            NIDEventModel(
                type = WINDOW_LOAD,
                attrs = listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "postCreated",
                        "className" to currentActivityName
                    )
                )
            )
        )
    }

    override fun onActivityPaused(activity: Activity) {
        NIDLog.d( msg="Activity - Paused")
        val currentActivityName = activity::class.java.name

        dataStore.saveEvent(
            NIDEventModel(
                type = WINDOW_BLUR,
                attrs = listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "paused",
                        "className" to currentActivityName
                    )
                )
            )
        )
    }

    override fun onActivityResumed(activity: Activity) {
        NIDLog.d(msg="Activity - Resumed")

        val currentActivityName = activity::class.java.name

        dataStore.saveEvent(
            NIDEventModel(
                type = WINDOW_FOCUS,
                attrs = listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "resumed",
                        "className" to currentActivityName
                    )
                )
            )
        )

        // depending on RN or Android run the following code
        registrationHelpers {
            activitiesStarted++

            registrationHelper.registerTargetFromScreen(
                activity,
                registerTarget = true,
                registerListeners = true,
                activityOrFragment = "activity",
                parent = currentActivityName
            )

            registrationHelper.registerWindowListeners(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        NIDLog.d( msg="Activity - Stopped")
        activitiesStarted--
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No Operation
        NIDLog.d( msg="Activity - Save Instance")
    }

    override fun onActivityDestroyed(activity: Activity) {
        NIDLog.d( msg="Activity - Destroyed")
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)

        NIDLog.d( msg="Activity - Destroyed - Window Unload")
        dataStore.saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    attrs = listOf(
                        mapOf(
                            "component" to "activity",
                            "lifecycle" to "destroyed",
                            "className" to activityDestroyed
                        )
                    )
                )
            )
    }
}