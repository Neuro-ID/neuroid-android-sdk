package com.neuroid.tracker.callbacks

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONArray
import org.json.JSONObject

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
            NIDLog.d("Neuro ID", "NIDDebug onActivityStarted existActivity.not()");

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

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "postCreated")
        metadataObj.put("className", "$currentActivityName")
        val attrJSON = JSONArray().put(metadataObj)
        NIDLog.d("NID--Activity", "Activity - POST Created - Window Load")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = attrJSON
                )
            )
    }

    override fun onActivityResumed(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Resumed")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "resumed")
        metadataObj.put("className", "${activity::class.java.name}")
        val attrJSON = JSONArray().put(metadataObj)

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_FOCUS,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = attrJSON
                )
            )
    }

}