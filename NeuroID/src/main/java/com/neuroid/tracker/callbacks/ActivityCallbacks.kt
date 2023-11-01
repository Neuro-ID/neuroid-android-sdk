package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

abstract class ActivityCallbacks: ActivityLifecycleCallbacks {
    var activitiesStarted = 0
    var listActivities = ArrayList<String>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        NIDLog.d("Neuro ID", "NIDDebug onActivityCreated");
    }

    abstract override fun onActivityStarted(activity: Activity)

    /**
     * Option for customers to force start with Activity
     */
    fun forceStart(activity: Activity) {
        registerTargetFromScreen(
            activity,
            NIDLogWrapper(),
            getDataStoreInstance(),
            true,
            true,
            activityOrFragment = "activity",
            parent = activity::class.java.simpleName
        )
        // register listeners for focus, blur and touch events
        registerWindowListeners(activity)
    }
    abstract override fun onActivityResumed(activity: Activity)

    override fun onActivityPaused(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Paused")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "paused")
        metadataObj.put("className", "${activity::class.java.simpleName}")
        val attrJSON = JSONArray().put(metadataObj)

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_BLUR,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = attrJSON
                )
            )
    }

    override fun onActivityStopped(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Stopped")
        activitiesStarted--
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No Operation
        NIDLog.d("NID--Activity", "Activity - Save Instance")
    }

    override fun onActivityDestroyed(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Destroyed")
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "destroyed")
        metadataObj.put("className", "${activity::class.java.simpleName}")
        val attrJSON = JSONArray().put(metadataObj)

        NIDLog.d("NID--Activity", "Activity - Destroyed - Window Unload")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = attrJSON
                )
            )
    }
}