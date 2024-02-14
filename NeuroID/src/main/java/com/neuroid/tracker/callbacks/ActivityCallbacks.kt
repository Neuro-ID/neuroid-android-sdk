package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.events.registerTargetFromScreen
import com.neuroid.tracker.events.registerWindowListeners
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper

abstract class ActivityCallbacks: ActivityLifecycleCallbacks {
    var activitiesStarted = 0
    var listActivities = ArrayList<String>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        NIDLog.d(msg = "onActivityCreated");
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
        NIDLog.d( msg="Activity - Paused")

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_BLUR,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = listOf(
                        mapOf(
                            "component" to "activity",
                            "lifecycle" to "paused",
                            "className" to "${activity::class.java.simpleName}"
                        )
                    )
                )
            )
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
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        val activityDestroyed = activity::class.java.name
        listActivities.remove(activityDestroyed)

        NIDLog.d( msg="Activity - Destroyed - Window Unload")
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    attrs = listOf(
                        mapOf(
                            "component" to "activity",
                            "lifecycle" to "destroyed",
                            "className" to "${activity::class.java.simpleName}"
                        )
                    )
                )
            )
    }
}