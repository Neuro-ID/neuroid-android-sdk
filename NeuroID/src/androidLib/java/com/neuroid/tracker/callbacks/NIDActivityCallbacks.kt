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
//        NIDLog.d("NID--Activity", "Activity - POST Created")

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

//            NIDLog.d("NID--Activity", "Activity - POST Created - REGISTER FRAGMENT LIFECYCLES")
            fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(wasChanged), true)
        }

        if (wasChanged) {
//            NIDLog.d("NID--Activity", "Activity - POST Created - Orientation change")
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


        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "postCreated")
        jsonObject.put("className", "${activity::class.java.simpleName}")

//        NIDLog.d("NID--Activity", "Activity - POST Created - Window Load")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    metadata = jsonObject
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
//        NIDLog.d("NID--Activity", "Activity - Started")
    }

    override fun onActivityPostStarted(activity: Activity) {
        super.onActivityPostStarted(activity)
//        NIDLog.d("NID--Activity", "Activity - POST Started")

        var cameBackFromBehind = false
        NIDLog.d("Neuro ID", "NIDDebug onActivityStarted");
        if (activitiesStarted == 0) {
            cameBackFromBehind = true
//            val gyroData = NIDSensorHelper.getGyroscopeInfo()
//            val accelData = NIDSensorHelper.getAccelerometerInfo()
//
//            val jsonObject = JSONObject()
//            jsonObject.put("component", "activity")
//            jsonObject.put("lifecycle", "postStarted")
//
//
////            NIDLog.d("NID--Activity", "Activity - POST Started - Window Focus")
//            getDataStoreInstance()
//                .saveEvent(
//                    NIDEventModel(
//                        type = WINDOW_FOCUS,
//                        ts = System.currentTimeMillis(),
//                        gyro = gyroData,
//                        accel = accelData,
//                        metadata = jsonObject
//                    )
//                )
        }
        activitiesStarted++

        val currentActivityName = activity::class.java.name
        val existActivity = listActivities.contains(currentActivityName)

        val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
        val hasFragments = fragManager?.hasFragments() ?: false

        if (existActivity) {
            if (hasFragments.not() && cameBackFromBehind.not()) {
//                NIDLog.d("NID--Activity", "Activity - POST Started - Exist & fragments")
                registerTargetFromScreen(
                    activity,
                    registerListeners = false,
                    activityOrFragment = "activity",
                    parent = currentActivityName
                )
            } else {
//                NIDLog.d("NID--Activity", "Activity - POST Started - Exist & no fragment")
                NIDLog.d("Neuro ID", "NIDDebug Activity has no fragments");
            }
        } else {
            listActivities.add(currentActivityName)
            if (hasFragments.not()) {
//                NIDLog.d("NID--Activity", "Activity - POST Started - NOT Exist & fragment")
                registerTargetFromScreen(
                    activity,
                    wasChanged.not(),
                    activityOrFragment = "activity",
                    parent = currentActivityName
                )
            } else {
//                NIDLog.d("NID--Activity", "Activity - POST Started - NOT Exist & no fragment")
                NIDLog.d("Neuro ID", "NIDDebug Activity does not exist, no fragments");
            }
            wasChanged = false
            registerWindowListeners(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        //No op
        // SHOULD BE WINDOW FOCUS?
//        NIDLog.d("NID--Activity", "Activity - Resumed")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "resumed")
        jsonObject.put("className", "${activity::class.java.simpleName}")

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_FOCUS,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    metadata = jsonObject
                )
            )
    }

    override fun onActivityPaused(activity: Activity) {
        //No op
        // SHOULD BE WINDOW BLUR?
//        NIDLog.d("NID--Activity", "Activity - Paused")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "paused")
        jsonObject.put("className", "${activity::class.java.simpleName}")

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_BLUR,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    metadata = jsonObject
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

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "destroyed")
        jsonObject.put("className", "${activity::class.java.simpleName}")

//        NIDLog.d("NID--Activity", "Activity - Destroyed - Window Unload")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    metadata = jsonObject
                )
            )
    }
}