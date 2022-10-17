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
    private var clientKey: String
) {
    private var firstTime = true
    private var endpoint = ENDPOINT_PRODUCTION
    private var sessionID = ""
    private var clientID = ""
    private var userID = ""
    private var timestamp: Long = 0L

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

    data class Builder(
        var application: Application? = null,
        var clientKey: String = ""
    ) {
        fun build() =
            NeuroID(application, clientKey)
    }

    companion object {

        private const val ENVIRONMENT_PRODUCTION = "LIVE"
        const val ENDPOINT_PRODUCTION = "https://receiver.neuroid.cloud/c"
        private const val ENDPOINT_DEVELOPMENT = "https://receiver.neuro-dev.com/c"

        private var singleton: NeuroID? = null

        @JvmStatic
        fun setNeuroIdInstance(neuroId: NeuroID) {
            if (singleton == null) {
                singleton = neuroId
                singleton?.setupCallbacks()
            }
        }

        fun getInstance(): NeuroID? = singleton
    }

    fun setUserID(userId: String) {
        userID = userId
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

    fun getUserId() = userID

    fun setScreenName(screen: String) {
        NIDServiceTracker.screenName = screen.replace("\\s".toRegex(),"%20")
    }

    fun excludeViewByResourceID(id: String) {
        application?.let {
            getDataStoreInstance().addViewIdExclude(id)
        }
    }

    fun setEnvironment(environment: String) {
        NIDServiceTracker.environment = environment
        endpoint = when (environment) {
            ENVIRONMENT_PRODUCTION -> ENDPOINT_PRODUCTION
            else -> ENDPOINT_DEVELOPMENT
        }
    }

    fun getEnvironment(): String = NIDServiceTracker.environment

    fun setSiteId(siteId: String) {
        NIDServiceTracker.siteId = siteId
    }

    fun getSiteId(): String =
        NIDServiceTracker.siteId

    fun getSessionId(): String {
        return sessionID
    }

    fun getClientId(): String {
        return clientID
    }

    fun getTabId(): String = NIDServiceTracker.rndmId

    fun getFirstTS(): Long = timestamp

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

    fun configureWithOptions(clientKey: String, endpoint: String?) {
        this.endpoint = endpoint ?: ENDPOINT_PRODUCTION
        this.clientKey = clientKey
        NIDServiceTracker.rndmId = ""
    }

    fun start() {
        NIDServiceTracker.rndmId = NIDSharedPrefsDefaults.getHexRandomID()

        CoroutineScope(Dispatchers.IO).launch {
            getDataStoreInstance().clearEvents() // Clean Events ?
            createSession()
        }
        application?.let {
            NIDJobServiceManager.startJob(it, clientKey, endpoint)
        }
    }

    fun stop() {
        NIDJobServiceManager.stopJob()
    }

    fun isStopped() = NIDJobServiceManager.isStopped()

    private suspend fun createSession() {
        timestamp = System.currentTimeMillis()
        application?.let {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            sessionID = sharedDefaults.getNewSessionID()
            clientID = sharedDefaults.getClientId(true)
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = sessionID,
                    lsid = "null",
                    cid = clientID,
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
                    ts = timestamp,
                    gyro = gyroData,
                    accel = accelData,
                    sw = NIDSharedPrefsDefaults.getDisplayWidth().toFloat(),
                    sh = NIDSharedPrefsDefaults.getDisplayHeight().toFloat()
                )
            )
        }
    }


}