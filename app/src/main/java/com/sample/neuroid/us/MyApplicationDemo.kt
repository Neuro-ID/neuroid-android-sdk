package com.sample.neuroid.us

import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroID

class MyApplicationDemo: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        val neuroId = NeuroID.Builder(this, "key_live_vtotrandom_form_mobilesandbox")
            .setTimeInSeconds(6)
            .build()
        NeuroID.setNeuroIdInstance(neuroId)
        NeuroID.getInstance().start()
    }
}