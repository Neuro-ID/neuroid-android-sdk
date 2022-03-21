package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var actualOrientation = 0
    //private var sensorManager: SensorManager? = null
    //private var mAccelerometer: Sensor? = null
    //private val sensorListener = NIDSensorListener()

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        NIDServiceTracker.screenName = activity::class.java.name
        NIDServiceTracker.screenFragName = ""
        val orientation = activity.resources.configuration.orientation

        (activity as? AppCompatActivity)?.supportFragmentManager
            ?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)
        registerViewsEventsForActivity(activity)

        if (actualOrientation == 0) {
            actualOrientation = orientation
        } else if (actualOrientation != orientation) {
            val strOrientation = if (actualOrientation == 1) {
                "Landscape"
            } else {
                "Portrait"
            }

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_ORIENTATION_CHANGE,
                    ts = System.currentTimeMillis(),
                    tg = hashMapOf(
                        "orientation" to strOrientation
                    )
                ))
            actualOrientation = orientation
        }

        //sensorManager = activity.getSystemService(SENSOR_SERVICE) as SensorManager
        //mAccelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onActivityStarted(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_LOAD,
                url = activity::class.java.name,
                ts = System.currentTimeMillis()
            ))
    }

    override fun onActivityResumed(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_FOCUS,
                url = activity::class.java.name,
                ts = System.currentTimeMillis()
            ))
        //sensorManager?.registerListener(sensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onActivityPaused(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_BLUR,
                url = activity::class.java.name,
                ts = System.currentTimeMillis()
            ))
        //sensorManager?.unregisterListener(sensorListener)
    }

    override fun onActivityStopped(activity: Activity) {
        // No Operation
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_UNLOAD,
                url = activity::class.java.name,
                ts = System.currentTimeMillis()
            ))
        unRegisterListenerFromActivity(activity)
    }
}