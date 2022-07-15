package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.NeuroID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

object NIDJobServiceManager {
    private var jobCaptureEvents: Job? = null

    @Volatile
    var userActive = true
    private var clientKey = ""
    private var application: Application? = null
    private var endpoint: String = ""

    fun startJob(application: Application, clientKey: String, endpoint: String) {
        this.clientKey = clientKey
        this.endpoint = endpoint
        this.application = application
        jobCaptureEvents = createJobServer()
    }

    fun restart() {
        jobCaptureEvents = createJobServer()
    }

    private fun createJobServer(): Job {
        val timeMills = 5000L
        return CoroutineScope(Dispatchers.IO).launch {
            NeuroID.getInstance().nidDataStoreManager.getNIDPreferences().filter { nidPreferences ->
                nidPreferences.events.isNotEmpty()
            }.debounce(timeMills)
                .collect {
                    val response = NIDServiceTracker.sendEventToServer(clientKey, endpoint, it)
                    if (response.second) {
                        userActive = false
                    }
                    NeuroID.getInstance().nidDataStoreManager.clearEvents()
                }
        }
    }

    fun stopJob() {
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }
}