package com.neuroid.tracker.callbacks

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_ORIENTATION_CHANGE
import com.neuroid.tracker.events.registerTargetFromScreen
import com.neuroid.tracker.events.registerWindowListeners
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper

class NIDActivityCallbacks: ActivityCallbacks() {
    private var auxOrientation = -1
    private var wasChanged = false

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

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (existActivity.not()) {
            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager

            NIDLog.d( msg="Activity - POST Created - REGISTER FRAGMENT LIFECYCLES")
            fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(wasChanged), true)
        }

        if (wasChanged) {
            NIDLog.d( msg="Activity - POST Created - Orientation change")
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_ORIENTATION_CHANGE,
                        ts = System.currentTimeMillis(),
                        o = "CHANGED",
                        gyro = gyroData,
                        accel = accelData
                    )
                )
            auxOrientation = orientation
        }

        NIDLog.d( msg="Activity - POST Created - Window Load")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = listOf(
                        mapOf(
                            "component" to "activity",
                            "lifecycle" to "postCreated",
                            "className" to "${activity::class.java.simpleName}"
                        )
                    )
                )
            )
    }

    override fun onActivityResumed(activity: Activity) {
        NIDLog.d( msg="Activity - Resumed")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_FOCUS,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = listOf(
                        mapOf(
                            "component" to "activity",
                            "lifecycle" to "resumed",
                            "className" to "${activity::class.java.simpleName}"
                        )
                    )
                )
            )

        activitiesStarted++

        val currentActivityName = activity::class.java.name

        registerTargetFromScreen(
            activity,
            NIDLogWrapper(),
            getDataStoreInstance(),
            registerTarget = true,
            registerListeners = true,
            activityOrFragment = "activity",
            parent = currentActivityName
        )

        registerWindowListeners(activity)
    }

}