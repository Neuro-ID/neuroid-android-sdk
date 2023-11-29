package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_ORIENTATION_CHANGE
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.events.registerTargetFromScreen
import com.neuroid.tracker.events.registerWindowListeners
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import org.json.JSONArray
import org.json.JSONObject

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

        NIDServiceTracker.screenActivityName = currentActivityName
        if (NIDServiceTracker.firstScreenName.isNullOrEmpty()) {
            NIDServiceTracker.firstScreenName = currentActivityName
        }
        if (NIDServiceTracker.screenFragName.isNullOrEmpty()) {
            NIDServiceTracker.screenFragName = ""
        }
        if (NIDServiceTracker.screenName.isNullOrEmpty()) {
            NIDServiceTracker.screenName = "AppInit"
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

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "postCreated")
        metadataObj.put("className", "${activity::class.java.simpleName}")
        val attrJSON = JSONArray().put(metadataObj)
        NIDLog.d( msg="Activity - POST Created - Window Load")
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
        NIDLog.d( msg="Activity - Resumed")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "resumed")
        metadataObj.put("className", "${activity::class.java.simpleName}")
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

        activitiesStarted++

        val currentActivityName = activity::class.java.name

        NeuroID.getInstance()?.clipboardManager?.let {
            registerTargetFromScreen(
                activity,
                NIDLogWrapper(),
                getDataStoreInstance(),
                it,
                registerTarget = true,
                registerListeners = true,
                activityOrFragment = "activity",
                parent = currentActivityName
            )
        }

        registerWindowListeners(activity)
    }

}