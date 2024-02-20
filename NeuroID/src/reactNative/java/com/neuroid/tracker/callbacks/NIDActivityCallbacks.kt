package com.neuroid.tracker.callbacks

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog

class NIDActivityCallbacks() : ActivityCallbacks() {
    var auxOrientation = -1
    var wasChanged = false

    override fun onActivityStarted(activity: Activity) {
        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        val existActivity = listActivities.contains(currentActivityName)

        if (NIDServiceTracker.screenActivityName.isNullOrEmpty()) {
            NIDServiceTracker.screenActivityName = currentActivityName
        }
        if (NIDServiceTracker.screenFragName.isNullOrEmpty()) {
            NIDServiceTracker.screenFragName = ""
        }
        if (NIDServiceTracker.screenName.isNullOrEmpty()) {
            NIDServiceTracker.screenName = "AppInit"
        }

        val changedOrientation = auxOrientation != orientation
        wasChanged = changedOrientation

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (existActivity.not()) {
            NIDLog.d(msg="onActivityStarted existActivity.not()");

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
                        o = "CHANGED",
                        gyro = gyroData,
                        accel = accelData
                    )
                )
            auxOrientation = orientation
        }

        NIDLog.d(msg="Activity - POST Created - Window Load")
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
                            "className" to currentActivityName
                        )
                    )
                )
            )
    }

    override fun onActivityResumed(activity: Activity) {
        NIDLog.d(msg="Activity - Resumed")

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
                            "className" to activity::class.java.name
                        )
                    )
                )
            )
    }

}