package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.callbacks.NIDSensorHelper
import kotlinx.coroutines.*

object NIDJobServiceManager {

    private var jobCaptureEvents: Job? = null

    @Volatile
    var userActive = true
    private var clientKey = ""
    private var application: Application? = null
    private var endpoint: String = ""

    fun startJob(
        application: Application,
        clientKey: String,
        endpoint: String
    ) {
        this.clientKey = clientKey
        this.endpoint = endpoint
        this.application = application
        jobCaptureEvents = createJobServer()
        NIDSensorHelper.initSensorHelper(application)
    }

    fun restart() {
        NIDSensorHelper.restartSensors()
        jobCaptureEvents = createJobServer()
    }

    private fun createJobServer(): Job {
        val timeMills = 5000L
        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive) {
                delay(timeMills)

                application?.let {
                    val response = NIDServiceTracker.sendEventToServer(clientKey, endpoint, it)
                    if (response.second) {
                        userActive = false
                    }
                } ?: run {
                    userActive = false
                }
            }
        }
    }

    fun stopJob() {
        NIDSensorHelper.stopSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }
}