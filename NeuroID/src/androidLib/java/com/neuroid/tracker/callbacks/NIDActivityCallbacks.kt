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
import org.json.JSONArray
import org.json.JSONObject

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var auxOrientation = -1
    private var activitiesStarted = 0
    private var listActivities = ArrayList<String>()
    private var wasChanged = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        NIDLog.d("Neuro ID", "NIDDebug onActivityCreated");
    }

    override fun onActivityStarted(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Created")

        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        if (auxOrientation == -1) {
            auxOrientation = orientation
        }
        val existActivity = listActivities.contains(currentActivityName)

        NIDServiceTracker.screenActivityName = currentActivityName
        if (NIDServiceTracker.firstScreenName.isBlank()) {
            NIDServiceTracker.firstScreenName = currentActivityName
        }
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"
        wasChanged = auxOrientation != orientation

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (existActivity.not()) {
            val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager

            NIDLog.d("NID--Activity", "Activity - POST Created - REGISTER FRAGMENT LIFECYCLES")
            fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(wasChanged), true)
        }

        if (wasChanged) {
            NIDLog.d("NID--Activity", "Activity - POST Created - Orientation change")
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

    /**
     * Option for customers to force start with Activity
     */
    fun forceStart(activity: Activity) {
        registerTargetFromScreen(
            activity,
            activityOrFragment = "activity",
            parent = activity::class.java.simpleName
        )
        // register listeners for focus, blur and touch events
        registerWindowListeners(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        //No op
        // SHOULD BE WINDOW FOCUS?
        NIDLog.d("NID--Activity", "Activity - Resumed")

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

        registerTargetFromScreen(
            activity,
            registerTarget = true,
            registerListeners = true,
            activityOrFragment = "activity",
            parent = currentActivityName
        )

        registerWindowListeners(activity)

    }

    override fun onActivityPaused(activity: Activity) {
        //No op
        // SHOULD BE WINDOW BLUR?
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