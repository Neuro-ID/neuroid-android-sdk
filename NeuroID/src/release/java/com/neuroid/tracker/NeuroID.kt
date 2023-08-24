package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.events.identifyView
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.extensions.startIntegrationHealthCheck
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.storage.initDataStoreCtx
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.NIDSingletonIDs
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.utils.getGUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeuroID private constructor(
    internal var application: Application?,
    private var clientKey: String
) {
    private var isSDKStarted = false
    private var firstTime = true
    private var endpoint = ENDPOINT_PRODUCTION
    private var sessionID = ""
    private var clientID = ""
    private var userID = ""
    private var timestamp: Long = 0L

    private var forceStart: Boolean? = null

    private var metaData: NIDMetaData? = null

    internal var verifyIntegrationHealth: Boolean = false
    internal var debugIntegrationHealthEvents: MutableList<NIDEventModel> =
        mutableListOf<NIDEventModel>()


    init {
        application?.let {
            metaData = NIDMetaData(it.applicationContext)
        }
    }

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
        const val ENDPOINT_PRODUCTION = "https://receiver.neuroid.cloud/c"

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

    internal fun validateUserId(userId: String) {
        val regex = "^[a-zA-Z0-9-_.]{3,100}$"

        if (!userId.matches(regex.toRegex())) {
            throw IllegalArgumentException ("Invalid UserId");
        }
    }

    fun setUserID(userId: String) {
        if (!this.isSDKStarted) {
            throw IllegalArgumentException ("NeuroID SDK is not started");
        }

        this.validateUserId(userId)

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
        NIDServiceTracker.screenName = screen.replace("\\s".toRegex(), "%20")
        createMobileMetadata()
    }

    fun excludeViewByResourceID(id: String) {
        application?.let {
            getDataStoreInstance().addViewIdExclude(id)
        }
    }

    fun setEnvironment(environment: String) {
        NIDServiceTracker.environment = environment
    }

    fun setEnvironmentProduction(prod: Boolean) {
        if (prod) {
            NIDServiceTracker.environment = "LIVE"
        } else {
            NIDServiceTracker.environment = "TEST"

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

    fun getForceStart(): Boolean? {
        return forceStart
    }

    fun setForceStart(activity: Activity) {
        this.forceStart = true
        NIDActivityCallbacks().forceStart(activity)
    }

    fun getTabId(): String = NIDServiceTracker.rndmId

    fun getFirstTS(): Long = timestamp

    fun getJsonPayLoad(context: Context): String {
        return getDataStoreInstance().getJsonPayload(context)
    }

    fun resetJsonPayLoad() {
        getDataStoreInstance().resetJsonPayload()
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

        saveIntegrationHealthEvents()
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

        saveIntegrationHealthEvents()
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

        saveIntegrationHealthEvents()
    }

    fun configureWithOptions(clientKey: String, endpoint: String?) {
        this.endpoint = endpoint ?: ENDPOINT_PRODUCTION
        this.clientKey = clientKey
        NIDServiceTracker.rndmId = ""
    }

    fun start() {
        this.isSDKStarted = true
        NIDServiceTracker.rndmId = "mobile"
        NIDSingletonIDs.retrieveOrCreateLocalSalt()

        CoroutineScope(Dispatchers.IO).launch {
            startIntegrationHealthCheck()
            createSession()
            saveIntegrationHealthEvents()
        }
        application?.let {
            NIDJobServiceManager.startJob(it, clientKey, endpoint)
        }
    }

    fun stop() {
        this.isSDKStarted = false
        CoroutineScope(Dispatchers.IO).launch {
            NIDJobServiceManager.sendEventsNow(true)
            NIDJobServiceManager.stopJob()
            saveIntegrationHealthEvents()
        }
    }

    fun closeSession() {
        if (!isStopped()) {
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CLOSE_SESSION,
                    ct = "SDK_EVENT",
                    ts = System.currentTimeMillis()
                )
            )
            stop()
        }
    }

    fun resetClientId() {
        application?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            clientID = sharedDefaults.resetClientId()
        }
    }

    fun isStopped() = NIDJobServiceManager.isStopped()

    private fun createMobileMetadata() {
        timestamp = System.currentTimeMillis()
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        application?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = MOBILE_METADATA_ANDROID,
                    ts = timestamp,
                    gyro = gyroData,
                    accel = accelData,
                    sw = NIDSharedPrefsDefaults.getDisplayWidth().toFloat(),
                    sh = NIDSharedPrefsDefaults.getDisplayHeight().toFloat(),
                    metadata = metaData?.toJson(),
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
                )
            );
        }
    }
    fun registerTarget(activity: Activity, view: View, addListener: Boolean) {
        identifyView(view, activity.getGUID(), true, addListener)
    }

    /**
     * Provide public access to application context for other intenral NID functions
     */
    fun getApplicationContext(): Context? {
        return this.application?.applicationContext
    }

    private suspend fun createSession() {
        timestamp = System.currentTimeMillis()
        application?.let {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            sessionID = sharedDefaults.getNewSessionID()
            clientID = sharedDefaults.getClientId()
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
                    sh = NIDSharedPrefsDefaults.getDisplayHeight().toFloat(),
                    metadata = metaData?.toJson()
                )
            )
            createMobileMetadata()
        }
    }


}