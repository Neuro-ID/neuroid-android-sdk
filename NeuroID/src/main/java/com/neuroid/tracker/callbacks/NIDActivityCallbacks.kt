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

class NIDActivityCallbacks: ActivityLifecycleCallbacks {
    private var actualOrientation = 0
    //private var sensorManager: SensorManager? = null
    //private var mAccelerometer: Sensor? = null
    //private val sensorListener = NIDSensorListener()

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        NIDServiceTracker.screenActivityName = activity::class.java.name
        NIDServiceTracker.screenFragName = ""
        NIDServiceTracker.screenName = "AppInit"
        val orientation = activity.resources.configuration.orientation

        val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager

        fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)
        fragManager?.let {
            registerViewsEventsForActivity(activity, it.hasFragments().not())
        }

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
                ts = System.currentTimeMillis()
            ))
    }

    override fun onActivityResumed(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_FOCUS,
                ts = System.currentTimeMillis()
            ))
        //sensorManager?.registerListener(sensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onActivityPaused(activity: Activity) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_BLUR,
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
                ts = System.currentTimeMillis()
            ))
        unRegisterListenerFromActivity(activity)
    }
}