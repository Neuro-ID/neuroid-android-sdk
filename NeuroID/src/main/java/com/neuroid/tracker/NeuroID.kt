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
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.NIDSingletonIDs
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.utils.getGUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NeuroID private constructor(
    internal var application: Application?, internal var clientKey: String
) {
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

    internal var isRN = false

    init {
        application?.let {
            metaData = NIDMetaData(it.applicationContext)
        }

        if (!validateClientKey(clientKey)) {
            NIDLog.e(msg = "Invalid Client Key")
            clientKey = ""
        } else {
            if (clientKey.contains("_live_")) {
                NIDServiceTracker.environment = "LIVE"
            } else {
                NIDServiceTracker.environment = "TEST"
            }
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
        var application: Application? = null, var clientKey: String = ""
    ) {
        fun build() = NeuroID(application, clientKey)
    }

    companion object {
        var showLogs: Boolean = true
        var isSDKStarted = false
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

    internal fun validateClientKey(clientKey: String): Boolean {
        var valid = false
        val regex = "key_(live|test)_[A-Za-z0-9]+"

        if (clientKey.matches(regex.toRegex())) {
            valid = true
        }

        return valid
    }

    internal fun validateUserId(userId: String) {
        val regex = "^[a-zA-Z0-9-_.]{3,100}$"

        if (!userId.matches(regex.toRegex())) {
            throw IllegalArgumentException("Invalid UserId")
        }
    }

    fun setUserID(userId: String) {

        this.validateUserId(userId)
        userID = userId

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()
        application?.let {
            NIDSharedPrefsDefaults(it).setUserId(userId)
        }
        val userIdEvent = NIDEventModel(
            type = SET_USER_ID,
            uid = userId,
            ts = System.currentTimeMillis(),
            gyro = gyroData,
            accel = accelData
        )
        if (isSDKStarted) {
            getDataStoreInstance().saveEvent(
                userIdEvent
            )
        } else {
            getDataStoreInstance().queueEvent(userIdEvent)
        }

    }

    fun getUserId() = userID

    fun setScreenName(screen: String) {
        if (isSDKStarted) {
            NIDServiceTracker.screenName = screen.replace("\\s".toRegex(), "%20")
            createMobileMetadata()
        } else {
            throw IllegalArgumentException("NeuroID SDK is not started")
        }

    }

    fun excludeViewByResourceID(id: String) {
        application?.let {
            getDataStoreInstance().addViewIdExclude(id)
        }
    }

    fun setEnvironment(environment: String) {
        NIDLog.i(
            msg = "**** NOTE: setEnvironmentProduction METHOD IS DEPRECATED"
        )
    }

    fun setEnvironmentProduction(prod: Boolean) {
        NIDLog.i(
            msg = "**** NOTE: setEnvironmentProduction METHOD IS DEPRECATED"
        )
    }

    fun getEnvironment(): String = NIDServiceTracker.environment

    fun setSiteId(siteId: String) {
        NIDLog.i(
            msg = "**** NOTE: setSiteId METHOD IS DEPRECATED"
        )
        NIDServiceTracker.siteId = siteId
    }

    fun getSiteId(): String {
        NIDLog.i(
            msg = "**** NOTE: getSiteId METHOD IS DEPRECATED"
        )
        return ""
    }

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
        NIDLog.i(
            msg = "**** NOTE: formSubmit METHOD IS DEPRECATED AND IS NO LONGER REQUIRED"
        )

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
        NIDLog.i(
            msg = "**** NOTE: formSubmitSuccess METHOD IS DEPRECATED AND IS NO LONGER REQUIRED"
        )

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
        NIDLog.i(
            msg = "**** NOTE: formSubmitFailure METHOD IS DEPRECATED AND IS NO LONGER REQUIRED"
        )

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

    open fun start() {
        if (clientKey == "") {
            NIDLog.e(
                msg = "Missing Client Key - please call configure prior to calling start"
            )
            throw IllegalStateException("NeuroID SDK Missing Client API Key");
        }

        isSDKStarted = true
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
        getDataStoreInstance().saveAndClearAllQueuedEvents()
    }

    fun stop() {
        isSDKStarted = false
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
                    type = CLOSE_SESSION, ct = "SDK_EVENT", ts = System.currentTimeMillis()
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

    fun registerTarget(activity: Activity, view: View, addListener: Boolean) {
        identifyView(
            view, activity.getGUID(), NIDLogWrapper(), getDataStoreInstance(), true, addListener
        )
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
                    attrs = JSONArray().put(JSONObject().put("isRN", isRN))
                )
            )
        }
    }

    fun setIsRN() {
        this.isRN = true
    }

    fun enableLogging(enable: Boolean) {
        showLogs = enable
    }
}