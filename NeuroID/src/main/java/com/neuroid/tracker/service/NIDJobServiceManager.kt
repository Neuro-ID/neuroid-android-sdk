package com.neuroid.tracker.service

import android.app.Application
import kotlinx.coroutines.*

object NIDJobServiceManager {
    private var jobCaptureEvents: Job? = null
    @Volatile
    var userActive = true
    private var clientKey = ""
    private var application: Application? = null

    fun startJob(application: Application, clientKey: String) {
        this.clientKey = clientKey
        this.application = application
        jobCaptureEvents = createJobServer()
    }

    fun restart() {
        jobCaptureEvents = createJobServer()
    }

    private fun createJobServer():Job {
        val timeMills = 5000L

        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive) {
                delay(timeMills)

                application?.let {
                    val response = NIDServiceTracker.sendEventToServer(clientKey, it)
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
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }
}