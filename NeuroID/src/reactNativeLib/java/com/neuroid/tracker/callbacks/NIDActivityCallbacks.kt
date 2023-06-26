package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.hasFragments
import com.neuroid.tracker.utils.NIDLog


class NIDActivityCallbacks() : ActivityLifecycleCallbacks {
    private var auxOrientation = -1
    private var activitiesStarted = 1
    private var listActivities = ArrayList<String>()
    private var wasChanged = false

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {

    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        NIDLog.d("Neuro ID", "NIDDebug onActivityCreated");

        val currentActivityName = activity::class.java.name
        val orientation = activity.resources.configuration.orientation
        val existActivity = listActivities.contains(currentActivityName)

        NIDServiceTracker.screenActivityName = currentActivityName
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"

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

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "postCreated")
        jsonObject.put("className", "$currentActivityName")

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


    public fun forceStart(activity: Activity) {
        registerTargetFromScreen(
            activity,
            registerTarget = true,
            registerListeners = true,
            activityOrFragment = "activity",
            parent = activity::class.java.name
        )
    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        //No operation

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "resumed")
        jsonObject.put("className", "${activity::class.java.name}")

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
        //No operation


        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "paused")
        jsonObject.put("className", "${activity::class.java.name}")

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
        activitiesStarted--
//        if (activitiesStarted == 0) {
//            val gyroData = NIDSensorHelper.getGyroscopeInfo()
//            val accelData = NIDSensorHelper.getAccelerometerInfo()
//
//            getDataStoreInstance()
//                .saveEvent(
//                    NIDEventModel(
//                        type = WINDOW_BLUR,
//                        ts = System.currentTimeMillis(),
//                        gyro = gyroData,
//                        accel = accelData
//                    )
//                )
//        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)

        val jsonObject = JSONObject()
        jsonObject.put("component", "activity")
        jsonObject.put("lifecycle", "destroyed")
        jsonObject.put("className", "$activityDestroyed")

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