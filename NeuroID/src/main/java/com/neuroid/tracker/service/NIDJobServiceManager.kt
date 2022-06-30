package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDGyroscopeListener
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.utils.NIDLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.TimeUnit

object NIDJobServiceManager {

    private const val TAG = "NIDJobServiceManager"

    private var jobCaptureEvents: Job? = null
    private var jobCaptureSensor: Job? = null

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
        NIDSensorHelper.initSensorHelper(application)
        jobCaptureSensor = createJobSensor()
    }

    fun restart() {
        jobCaptureEvents = createJobServer()
        jobCaptureSensor = createJobSensor()
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

    private fun createJobSensor(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            NIDSensorHelper.getSensorInfo().collect {
                NIDLog.d(
                    TAG,
                    "Gyroscope axisX: ${it.gyroscopeData.axisX} axisY: ${it.gyroscopeData.axisY} axisZ: ${it.gyroscopeData.axisZ}"
                )
                NIDLog.d(
                    TAG,
                    "Accelerometer axisX: ${it.accelerometer.axisX} axisY: ${it.accelerometer.axisY} axisZ: ${it.accelerometer.axisZ}"
                )
                NeuroID.getInstance().nidSensors = it
            }
        }
    }

    fun stopJob() {
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
        jobCaptureSensor?.cancel()
        jobCaptureSensor = null
    }
}