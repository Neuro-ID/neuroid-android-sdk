package com.neuroid.example

import android.app.Application
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDConfiguration
import com.neuroid.tracker.models.NIDRegion

class ApplicationMain : Application() {
    companion object {
        val dataManager = DataManager()
        var isApplicationSubmitted = false
        var isLoggedIn = false

        var registeredSessionId = 1
        var registeredUserID = 1
        var registeredUserName = "regUser-"
        var sessionName = "sessionName-"
    }

    fun resetAll() {
        // userEnteredId = ""
        isApplicationSubmitted = false
        isLoggedIn = false
    }

    override fun onCreate() {
        super.onCreate()

        // register the push notification channel for the app, so
        // that the simulated push can be received and displayed
        NotificationHelper.createChannel(this)

        NeuroID.BuilderConfig(
            this,
            NIDConfiguration("key_live_MwC5DQNYzRsRhnnYjvz1fJtp",
                advancedDeviceKey = "KnJvMIBAqxp7PRJiOmil",
                useAdvancedDeviceProxy = true, isAdvancedDevice = true,
                serverEnvironment = NeuroID.TEST,
                region = NIDRegion.TEST)
        ).build()
    }

    fun setUserID(userId: String) {
        NeuroID.getInstance()?.setUserID(userId)
    }

    fun stopTracking() {
        NeuroID.getInstance()?.stopSession()
    }

    override fun onTerminate() {
        NeuroID.getInstance()?.stop()
        super.onTerminate()
    }

}
