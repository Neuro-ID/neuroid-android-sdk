package com.neuroid.tracker

import android.app.Application
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.events.CREATE_SESSION
import com.neuroid.tracker.events.FORM_SUBMIT
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.service.NIDServiceTracker.NID_ERROR_SERVICE
import com.neuroid.tracker.service.NIDServiceTracker.NID_ERROR_SYSTEM
import com.neuroid.tracker.service.NIDServiceTracker.NID_NO_EVENTS_ALLOWED
import com.neuroid.tracker.service.NIDServiceTracker.NID_OK_SERVICE
import com.neuroid.tracker.service.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.storage.initDataStoreCtx
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTimerActive
import kotlinx.coroutines.*

class NeuroID private constructor(
    private var application: Application?,
    private var clientKey: String,
    private val timeInSeconds: Int
) {
    private var firstTime = true

    @Synchronized
    private fun setupCallbacks() {
        if (firstTime) {
            firstTime = false
            application?.let {
                initDataStoreCtx(it.applicationContext)
                it.registerActivityLifecycleCallbacks(NIDActivityCallbacks())
                NIDTimerActive.initTimer()
            }
        }
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
            singleton.setupCallbacks()
        }

        fun getInstance() = singleton
    }

    fun captureEvent(eventName: String, tgs: String) {
        application?.applicationContext?.let {
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = eventName,
                    tgs = tgs,
                    ts = System.currentTimeMillis()
                ).getOwnJson()
            )
        }
    }

    fun formSubmit() {
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT,
                ts = System.currentTimeMillis()
            ).getOwnJson()
        )
    }

    fun formSubmitSuccess() {
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_SUCCESS,
                ts = System.currentTimeMillis()
            ).getOwnJson()
        )
    }

    fun formSubmitFailure() {
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_FAILURE,
                ts = System.currentTimeMillis()
            ).getOwnJson()
        )
    }

    fun start() {
        getDataStoreInstance().getAllEvents() // Clean Events ?
        createSession()
        jobCaptureEvents = startLoopCaptureEvent()
    }

    fun stop() {
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }

    private fun createSession() {
        application?.applicationContext?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)

            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = sharedDefaults.getSessionID(),
                    lsid = "null",
                    cid = sharedDefaults.getClientId(),
                    did = sharedDefaults.getDeviceId(),
                    iid = sharedDefaults.getIntermediateId(),
                    loc = sharedDefaults.getLocale(),
                    ua = sharedDefaults.getUserAgent(),
                    tzo = sharedDefaults.getTimeZone(),
                    lng = sharedDefaults.getLocale(),
                    ce = true,
                    je = true,
                    ol = true,
                    p = sharedDefaults.getPlatform(),
                    jsl = listOf("na"),
                    dnt = false,
                    url = "",
                    ns = "nid",
                    jsv = "null",
                    ts = System.currentTimeMillis()
                ).getOwnJson()
            )
        }
    }

    private fun startLoopCaptureEvent(): Job {
        val timeMills: Long = if(timeInSeconds < 1) { 1000 } else { timeInSeconds.toLong() * 1000 }

        return CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(timeMills)

                application?.applicationContext?.let { context ->
                    CoroutineScope(Dispatchers.IO).launch {
                        when(NIDServiceTracker.sendEventToServer(clientKey, context)) {
                            NID_ERROR_SERVICE -> NIDLog.e("NeuroId", "Error service")
                            NID_ERROR_SYSTEM -> NIDLog.e("NeuroId", "Error system")
                            NID_NO_EVENTS_ALLOWED -> NIDLog.d("NeuroId", "No events allowed")
                            NID_OK_SERVICE -> NIDLog.d("NeuroId", "OK service")
                        }
                    }
                }
            }
        }
    }
}