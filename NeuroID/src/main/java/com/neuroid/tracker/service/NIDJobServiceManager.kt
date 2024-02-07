package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.utils.NIDLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object NIDJobServiceManager {

    private var jobCaptureEvents: Job? = null
    private var jobSendEvents: Job? = null
    var isSendEventsNowEnabled = true

    @Volatile
    var userActive = true
    private var clientKey = ""
    private var application: Application? = null
    private var endpoint: String = ""

    private val sendEventsNotification = Channel<Boolean>(Channel.UNLIMITED)

    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
        endpoint: String
    ) {
        this.clientKey = clientKey
        this.endpoint = endpoint
        this.application = application
        jobCaptureEvents = createJobServer()
        jobSendEvents = createSendEventsServer()
        NIDSensorHelper.initSensorHelper(application)
    }

    @Synchronized
    fun restart() {
        NIDSensorHelper.restartSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = createJobServer()
    }

    /**
     * Creates a job that synchronizes all requests to send events.
     */
    private fun createSendEventsServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
            for (notification in sendEventsNotification) {
                var sendNow = notification
                var next = sendEventsNotification.tryReceive()
                while (next.isSuccess) {
                    next.getOrNull()?.let {
                        sendNow = sendNow || it
                    }
                    next = sendEventsNotification.tryReceive()
                }
                sendEventsNow(sendNow)
            }
            } catch (exception: Exception) {
                NIDLog.e("NeuroID", exception.toString())
            }
            NIDLog.e("NeuroID", "send event job exited")
        }
    }

    private fun createJobServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive && isActive) {
                delay(5000L)
                sendEventsNotification.send(false)
            }
        }
    }

    /**
     * Notify the send events server to initiate a send.
     */
    fun sendEvents(forceSendEvents: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            sendEventsNotification.send(forceSendEvents)
        }
    }

    private suspend fun sendEventsNow(forceSendEvents: Boolean = false) {
        if (forceSendEvents || (isSendEventsNowEnabled && !isStopped())) {
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

    @Synchronized
    fun stopJob() {
        NIDSensorHelper.stopSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }

    @Synchronized
    fun isStopped(): Boolean {
        return jobCaptureEvents?.isActive != true
    }
}