package com.neuroid.tracker.callbacks

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.registerViewsEventsForActivity
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NeuroIdActivityCallbacks: ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        (activity as? AppCompatActivity)?.supportFragmentManager
            ?.registerFragmentLifecycleCallbacks(NeuroIdFragmentCallbacks(), true)
        registerViewsEventsForActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        getDataStoreInstance(activity.applicationContext)
            .saveEvent(NIDEventModel(type = WINDOW_LOAD, ts = System.currentTimeMillis()).getOwnJson())
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        //No Operation
    }

    override fun onActivityPaused(activity: Activity) {
        getDataStoreInstance(activity.applicationContext)
            .saveEvent(NIDEventModel(type = USER_INACTIVE, ts = System.currentTimeMillis()).getOwnJson())
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        // No Operation
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // No Operation
    }

    override fun onActivityDestroyed(activity: Activity) {
        getDataStoreInstance(activity.applicationContext)
            .saveEvent(NIDEventModel(type = WINDOW_UNLOAD, ts = System.currentTimeMillis()).getOwnJson())
        Log.d("NeuroId", "Activity:${activity::class.java.name} is onActivityDestroyed")
    }
}