package com.neuroid.tracker

import android.app.Application
import android.util.Log
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.service.NIDServiceTracker.NID_ERROR_SERVICE
import com.neuroid.tracker.service.NIDServiceTracker.NID_ERROR_SYSTEM
import com.neuroid.tracker.service.NIDServiceTracker.NID_NO_EVENTS_ALLOWED
import com.neuroid.tracker.service.NIDServiceTracker.NID_OK_SERVICE
import com.neuroid.tracker.storage.getDataStoreInstance
import kotlinx.coroutines.*

class NeuroID private constructor(
    private var application: Application?,
    private var clientKey: String,
    private val timeInSeconds: Int
) {
    init {
        application?.registerActivityLifecycleCallbacks(NIDActivityCallbacks())
    }

    private var jobCaptureEvents: Job? = null

    data class Builder(
        var application: Application? = null,
        var clientKey: String = "",
        var timeInSeconds: Int = 1 //Define default
    ) {
        fun setTimeInSeconds(timeInSeconds: Int) = apply { this.timeInSeconds = timeInSeconds }
        fun build() =
            NeuroID(application, clientKey, timeInSeconds)
    }

    companion object {
        private lateinit var singleton: NeuroID
        fun setNeuroIdInstance(neuroId: NeuroID) {
            singleton = neuroId
        }

        fun getInstance() = singleton
    }

    fun captureEvent(event: NIDEventModel) {
        application?.applicationContext?.let {
            getDataStoreInstance(it).saveEvent(event.getOwnJson())
        }
    }

    fun startToSendAllEvents() {
        jobCaptureEvents = startLoopCaptureEvent()
    }

    fun stopSendAllEvents() {
        jobCaptureEvents?.cancel()
    }

    private fun startLoopCaptureEvent(): Job {
        val timeMills: Long = if(timeInSeconds < 1) { 1000 } else { timeInSeconds.toLong() * 1000 }

        return CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(timeMills)

                application?.applicationContext?.let { context ->
                    CoroutineScope(Dispatchers.IO).launch {
                        when(NIDServiceTracker.sendEventToServer(clientKey, context)) {
                            NID_ERROR_SERVICE -> Log.d("NeuroId", "Error service")
                            NID_ERROR_SYSTEM -> Log.d("NeuroId", "Error system")
                            NID_NO_EVENTS_ALLOWED -> Log.d("NeuroId", "No events allowed")
                            NID_OK_SERVICE -> Log.d("NeuroId", "OK service")
                        }
                    }
                }
            }
        }
    }
}