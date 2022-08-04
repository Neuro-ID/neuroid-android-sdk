package com.neuroid.tracker

import android.app.Application
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.storage.initDataStoreCtx
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeuroID private constructor(
    private var application: Application?,
    private var clientKey: String,
    private var environment: String,
    private var siteId: String
) {
    private var firstTime = true
    private var endpoint = "https://receiver.neuro-dev.com/c"
    private var sessionID = ""

    @Synchronized
    private fun setupCallbacks() {
        if (firstTime) {
            firstTime = false
            application?.let {
                endpoint = when (environment) {
                    "PRODUCTION" -> "https:/receiver.neuroid.cloud/c"
                    else -> "https://receiver.neuro-dev.com/c"
                }

                initDataStoreCtx(it.applicationContext)
                it.registerActivityLifecycleCallbacks(NIDActivityCallbacks())
                NIDTimerActive.initTimer()
            }
        }
    }

    data class Builder(
        var application: Application? = null,
        var clientKey: String = "",
        private var environment: String = "PRODUCTION",
        private var siteId: String = ""
    ) {
        fun build() =
            NeuroID(application, clientKey, environment, siteId)
    }

    companion object {
        private lateinit var singleton: NeuroID

        @JvmStatic
        fun setNeuroIdInstance(neuroId: NeuroID) {
            singleton = neuroId
            singleton.setupCallbacks()
        }

        @JvmStatic
        fun getInstance() = singleton
    }

    fun setUserID(userId: String) {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        application?.let {
            NIDSharedPrefsDefaults(it).setUserId(userId)
        }
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = SET_USER_ID,
                uid = userId,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )
    }

    fun setScreenName(screen: String) {
        NIDServiceTracker.screenName = screen
    }

    fun excludeViewByResourceID(id: String) {
        application?.let {
            getDataStoreInstance().addViewIdExclude(id)
        }
    }

    fun getSessionId(): String {
        return sessionID
    }

    fun captureEvent(eventName: String, tgs: String) {
        application?.applicationContext?.let {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = eventName,
                    tgs = tgs,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData
                )
            )
        }
    }

    fun formSubmit() {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )
    }

    fun formSubmitSuccess() {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_SUCCESS,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )
    }

    fun formSubmitFailure() {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_FAILURE,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )
    }

    fun configureWithOptions(clientKey: String, endpoint: String) {
        this.endpoint = endpoint
        this.clientKey = clientKey
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            getDataStoreInstance().clearEvents() // Clean Events ?
            createSession()
        }
        application?.let {
            NIDJobServiceManager.startJob(it, clientKey, endpoint, environment, siteId)
        }
    }

    fun stop() {
        NIDJobServiceManager.stopJob()
    }


    private suspend fun createSession() {
        application?.let {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            sessionID = sharedDefaults.getNewSessionID()
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = sessionID,
                    lsid = "null",
                    cid = sharedDefaults.getClientId(),
                    did = sharedDefaults.getDeviceId(),
                    iid = sharedDefaults.getIntermediateId(),
                    loc = sharedDefaults.getLocale(),
                    ua = sharedDefaults.getUserAgent(),
                    tzo = sharedDefaults.getTimeZone(),
                    lng = sharedDefaults.getLanguage(),
                    ce = true,
                    je = true,
                    ol = true,
                    p = sharedDefaults.getPlatform(),
                    jsl = listOf(),
                    dnt = false,
                    url = "",
                    ns = "nid",
                    jsv = NIDVersion.getSDKVersion(),
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData
                )
            )
        }
    }


}