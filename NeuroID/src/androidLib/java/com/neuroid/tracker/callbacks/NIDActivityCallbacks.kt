package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.hasFragments
import org.json.JSONArray
import org.json.JSONObject

class NIDActivityCallbacks : ActivityLifecycleCallbacks {
    private var auxOrientation = -1
    private var activitiesStarted = 0
    private var listActivities = ArrayList<String>()
    private var wasChanged = false
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        NIDLog.d("NID--Activity", "Activity - Created")
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPostCreated(activity, savedInstanceState)
        NIDLog.d("NID--Activity", "Activity - POST Created")

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
    public fun forceStart(activity: Activity) {
        registerTargetFromScreen(
            activity,
            activityOrFragment = "activity",
            parent = activity::class.java.simpleName
        )
    }

    override fun onActivityStarted(activity: Activity) {
        NIDLog.d("NID--Activity", "Activity - Started")
    }

    override fun onActivityPostStarted(activity: Activity) {
        super.onActivityPostStarted(activity)
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

        var cameBackFromBehind = false
        if (activitiesStarted == 0) {
            cameBackFromBehind = true
        }
        activitiesStarted++

        val currentActivityName = activity::class.java.name
        val existActivity = listActivities.contains(currentActivityName)

        val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
        val hasFragments = fragManager?.hasFragments() ?: false

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
//        NIDLog.d("NID--Activity", "Activity - Paused")

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
//        NIDLog.d("NID--Activity", "Activity - Stopped")
        activitiesStarted--
//        if (activitiesStarted == 0) {
//            val gyroData = NIDSensorHelper.getGyroscopeInfo()
//            val accelData = NIDSensorHelper.getAccelerometerInfo()
//
//            val jsonObject = JSONObject()
//            jsonObject.put("component", "activity")
//            jsonObject.put("lifecycle", "stopped")
//
//
////            NIDLog.d("NID--Activity", "Activity - Stopped - Window Blur")
//            getDataStoreInstance()
//                .saveEvent(
//                    NIDEventModel(
//                        type = WINDOW_BLUR,
//                        ts = System.currentTimeMillis(),
//                        gyro = gyroData,
//                        accel = accelData,
//                        metadata = jsonObject
//                    )
//                )
//        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
//        NIDLog.d("NID--Activity", "Activity - Save Instance")
    }

    override fun onActivityDestroyed(activity: Activity) {
//        NIDLog.d("NID--Activity", "Activity - Destroyed")
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)

        val metadataObj = JSONObject()
        metadataObj.put("component", "activity")
        metadataObj.put("lifecycle", "destroyed")
        metadataObj.put("className", "${activity::class.java.simpleName}")
        val attrJSON = JSONArray().put(metadataObj)

//        NIDLog.d("NID--Activity", "Activity - Destroyed - Window Unload")
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