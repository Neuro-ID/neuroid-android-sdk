package com.neuroid.tracker

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.events.identifyView
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.extensions.startIntegrationHealthCheck
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.storage.initDataStoreCtx
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.NIDSingletonIDs
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.utils.generateUniqueHexId
import com.neuroid.tracker.utils.getGUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NeuroID private constructor(
    internal var application: Application?, internal var clientKey: String
) {
    @Volatile
    private var pauseCollectionJob: Job? = null

    private var firstTime = true
    internal var sessionID = ""
    internal var clientID = ""
    internal var userID = ""
    internal var registeredUserID = ""
    internal var timestamp: Long = 0L

    internal var forceStart: Boolean = false

    private var metaData: NIDMetaData? = null

    internal var verifyIntegrationHealth: Boolean = false
    internal var debugIntegrationHealthEvents: MutableList<NIDEventModel> =
        mutableListOf<NIDEventModel>()

    internal var isRN = false

    internal var NIDLog: NIDLogWrapper = NIDLogWrapper()
    internal var dataStore: NIDDataStoreManager = getDataStoreInstance()
    internal var nidActivityCallbacks: NIDActivityCallbacks = NIDActivityCallbacks()
    internal var nidJobServiceManager: NIDJobServiceManager
    internal var clipboardManager: ClipboardManager? = null

    internal var lowMemory:Boolean = false

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

        nidJobServiceManager = NIDJobServiceManager(
            NIDLog,
            dataStore
        )

    }

    @Synchronized
    private fun setupCallbacks() {
        if (firstTime) {
            firstTime = false
            application?.let {

                initDataStoreCtx(it.applicationContext)
                it.registerActivityLifecycleCallbacks(nidActivityCallbacks)
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

        internal val GYRO_SAMPLE_INTERVAL = 200L
        internal val captureGyroCadence = true

        private var singleton: NeuroID? = null

        @JvmStatic
        @Deprecated(
            "setNeuroIdInstance has been renamed to setNeuroIDInstance",
            ReplaceWith("NeuroID.setNeuroIDInstance()")
        )
        fun setNeuroIdInstance(neuroID: NeuroID) {
            this.setNeuroIDInstance(neuroID)
        }

        @JvmStatic
        fun setNeuroIDInstance(neuroID: NeuroID) {
            if (singleton == null) {
                singleton = neuroID
                singleton?.setupCallbacks()
            }
        }

        fun getInstance(): NeuroID? = singleton
    }

    internal fun setLoggerInstance(logger: NIDLogWrapper) {
        NIDLog = logger
    }

    internal fun setDataStoreInstance(store: NIDDataStoreManager) {
        dataStore = store
    }

    internal fun setClipboardManagerInstance(cm: ClipboardManager) {
        clipboardManager = cm
    }

    internal fun getClipboardManagerInstance(): ClipboardManager? {
        if (clipboardManager == null) {
            return getApplicationContext()
                ?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
        return clipboardManager
    }

    internal fun setNIDActivityCallbackInstance(callback: NIDActivityCallbacks) {
        nidActivityCallbacks = callback
    }

    internal fun setNIDJobServiceManager(serviceManager: NIDJobServiceManager) {
        nidJobServiceManager = serviceManager
    }

    @VisibleForTesting
    fun setTestURL(endpoint: String){
        nidJobServiceManager.endpoint = endpoint
    }

    internal fun validateClientKey(clientKey: String): Boolean {
        var valid = false
        val regex = "key_(live|test)_[A-Za-z0-9]+"

        if (clientKey.matches(regex.toRegex())) {
            valid = true
        }

        return valid
    }

    internal fun validateUserId(userId: String): Boolean {
        val regex = "^[a-zA-Z0-9-_.]{3,100}$"

        if (!userId.matches(regex.toRegex())) {
            NIDLog.e(msg = "Invalid UserID")
            return false
        }

        return true
    }

    fun setRegisteredUserID(registeredUserId: String): Boolean {
        val validID = setGenericUserID(
            SET_REGISTERED_USER_ID,
            registeredUserId,
        )

        if (!validID) {
            return false
        }

        this.registeredUserID = registeredUserId
        return true;
    }

    fun setUserID(userID: String): Boolean {
        return setUserID(userID, true)
    }

    private fun setUserID(userId: String, userGenerated: Boolean): Boolean {
        val validID = setGenericUserID(
            SET_USER_ID,
            userId,
            userGenerated
        )

        if (!validID) {
            return false
        }

        this.userID = userId
        return true;
    }

    internal fun setGenericUserID(
        type: String,
        genericUserId: String,
        userGenerated: Boolean = true
    ): Boolean {
        try {
            val validID = validateUserId(genericUserId)
            val originRes =
                getOriginResult(
                    genericUserId,
                    validID = validID,
                    userGenerated = userGenerated
                )
            sendOriginEvent(originRes)

            if (!validID) {
                return false
            }

            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            application?.let {
                when (type) {
                    SET_USER_ID -> NIDSharedPrefsDefaults(it).setUserId(genericUserId)
                    SET_REGISTERED_USER_ID -> NIDSharedPrefsDefaults(it).setRegisteredUserId(
                        genericUserId
                    )
                }
            }
            val genericUserIdEvent = NIDEventModel(
                type = type,
                uid = genericUserId,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
            if (isSDKStarted) {
                dataStore.saveEvent(
                    genericUserIdEvent
                )
            } else {
                dataStore.queueEvent(genericUserIdEvent)
            }

            return true
        } catch (exception: Exception) {
            NIDLog.e(msg = "failure processing user id! $type, $genericUserId $exception")
            return false;
        }
    }

    @Deprecated("Replaced with getUserID", ReplaceWith("getUserID()"))
    fun getUserId() = userID
    fun getUserID() = userID

    fun getRegisteredUserID() = registeredUserID

    fun setScreenName(screen: String): Boolean {

        if (!isSDKStarted) {
            NIDLog.e(msg = "NeuroID SDK is not started")
            return false
        }

        NIDServiceTracker.screenName = screen.replace("\\s".toRegex(), "%20")
        createMobileMetadata()

        return true
    }

    fun getScreenName(): String = NIDServiceTracker.screenName

    fun excludeViewByTestID(id: String) {
        application?.let {
            dataStore.addViewIdExclude(id)
        }
    }

    @Deprecated("setEnvironment is deprecated and no longer required")
    fun setEnvironment(environment: String) {
        NIDLog.i(
            msg = "**** NOTE: setEnvironment METHOD IS DEPRECATED"
        )
    }

    @Deprecated("setEnvironmentProduction is deprecated and no longer required")
    fun setEnvironmentProduction(prod: Boolean) {
        NIDLog.i(
            msg = "**** NOTE: setEnvironmentProduction METHOD IS DEPRECATED"
        )
    }

    fun getEnvironment(): String = NIDServiceTracker.environment

    @Deprecated("getSiteId is deprecated and no longer required")
    fun setSiteId(siteId: String) {
        NIDLog.i(
            msg = "**** NOTE: setSiteId METHOD IS DEPRECATED"
        )
        NIDServiceTracker.siteId = siteId
    }

    @Deprecated("getSiteId is deprecated")
    internal fun getSiteId(): String {
        NIDLog.i(
            msg = "**** NOTE: getSiteId METHOD IS DEPRECATED"
        )
        return ""
    }

    @Deprecated("Replaced with getSessionID", ReplaceWith("getSessionID()"))
    fun getSessionId(): String {
        return sessionID
    }

    fun getSessionID(): String = sessionID

    @Deprecated("Replaced with getClientID", ReplaceWith("getClientID()"))
    fun getClientId(): String {
        return clientID
    }

    fun getClientID(): String = clientID

    internal fun getForceStart(): Boolean? {
        return forceStart
    }

    fun registerPageTargets(activity: Activity) {
        this.forceStart = true
        nidActivityCallbacks.forceStart(activity)
    }

    internal fun getTabId(): String = NIDServiceTracker.rndmId

    internal fun getFirstTS(): Long = timestamp

    internal fun captureEvent(eventName: String, tgs: String) {
        application?.applicationContext?.let {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            dataStore.saveEvent(
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

    @Deprecated("formSubmit is deprecated and no longer required")
    fun formSubmit() {
        NIDLog.i(
            msg = "**** NOTE: formSubmit METHOD IS DEPRECATED"
        )

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        dataStore.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData,

                )
        )

        saveIntegrationHealthEvents()
    }

    @Deprecated("formSubmitSuccess is deprecated and no longer required")
    fun formSubmitSuccess() {
        NIDLog.i(
            msg = "**** NOTE: formSubmitSuccess METHOD IS DEPRECATED"
        )

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        dataStore.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_SUCCESS,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )

        saveIntegrationHealthEvents()
    }

    @Deprecated("formSubmitFailure is deprecated and no longer required")
    fun formSubmitFailure() {
        NIDLog.i(
            msg = "**** NOTE: formSubmitFailure METHOD IS DEPRECATED"
        )

        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        dataStore.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_FAILURE,
                ts = System.currentTimeMillis(),
                gyro = gyroData,
                accel = accelData
            )
        )

        saveIntegrationHealthEvents()
    }

    open fun start(): Boolean {
        if (clientKey == "") {
            NIDLog.e(
                msg = "Missing Client Key - please call configure prior to calling start"
            )
            return false
        }

        application?.let {
            nidJobServiceManager.startJob(it, clientKey)
        }
        isSDKStarted = true
        NIDServiceTracker.rndmId = "mobile"
        NIDSingletonIDs.retrieveOrCreateLocalSalt()

        CoroutineScope(Dispatchers.IO).launch {
            startIntegrationHealthCheck()
            createSession()
            saveIntegrationHealthEvents()
        }
        dataStore.saveAndClearAllQueuedEvents()

        return true
    }

    fun stop() {
        pauseCollection()
    }

    fun closeSession() {
        if (!isStopped()) {
            dataStore.saveEvent(
                NIDEventModel(
                    type = CLOSE_SESSION, ct = "SDK_EVENT", ts = System.currentTimeMillis()
                )
            )
            stop()
        }
    }

    internal fun resetClientId() {
        application?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            clientID = sharedDefaults.resetClientId()
        }
    }

    fun isStopped() = !isSDKStarted

    internal fun registerTarget(activity: Activity, view: View, addListener: Boolean) {
        identifyView(
            view,
            activity.getGUID(),
            NIDLogWrapper(),
            dataStore,
            true,
            addListener
        )
    }

    /**
     * Provide public access to application context for other intenral NID functions
     */
    internal fun getApplicationContext(): Context? {
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
            dataStore.saveEvent(
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
            dataStore.saveEvent(
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

    fun getSDKVersion() = NIDVersion.getSDKVersion()

    // new Session Commands
    fun clearSessionVariables() {
        userID = ""
        registeredUserID = ""
        this.getApplicationContext()?.let {
            NIDSharedPrefsDefaults(it).setUserId("")
            NIDSharedPrefsDefaults(it).setRegisteredUserId("")
        }
    }

    fun startSession(sessionID: String? = null): SessionStartResult {
        if (clientKey == "") {
            NIDLog.e(
                msg = "Missing Client Key - please call configure prior to calling start"
            )
            return SessionStartResult(false, "")
        }

        if (userID != "" || isSDKStarted) {
            stopSession()
        }

        var finalSessionID = sessionID ?: generateUniqueHexId();

        if (!setUserID(
                finalSessionID,
                sessionID != null
            )
        ) {
            return SessionStartResult(false, "")
        }

        isSDKStarted = true
        NIDServiceTracker.rndmId = "mobile"
        NIDSingletonIDs.retrieveOrCreateLocalSalt()

        CoroutineScope(Dispatchers.IO).launch {
            startIntegrationHealthCheck()
            createSession()
            saveIntegrationHealthEvents()
        }

        resumeCollection()

        dataStore.saveAndClearAllQueuedEvents()

        // we need to set finalSessionID with the set random user id
        // if a sessionID was not passed in
        finalSessionID = getUserID()

        return SessionStartResult(true, finalSessionID)
    }

    private fun sendOriginEvent(originResult: SessionIDOriginResult) {
        // sending these as individual items.
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = SET_VARIABLE,
                    ts = System.currentTimeMillis(),
                    key = "sessionIdCode",
                    v = originResult.originCode
                )
            )

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = SET_VARIABLE,
                    ts = System.currentTimeMillis(),
                    key = "sessionIdSource",
                    v = originResult.origin
                )
            )

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = SET_VARIABLE,
                    ts = System.currentTimeMillis(),
                    key = "sessionId",
                    v = originResult.sessionID
                )
            )
    }

    internal fun getOriginResult(
        sessionID: String,
        validID: Boolean,
        userGenerated: Boolean
    ): SessionIDOriginResult {
        var origin = if (userGenerated) {
            NID_ORIGIN_CUSTOMER_SET
        } else {
            NID_ORIGIN_NID_SET
        }

        var originCode = if (validID) {
            if (
                userGenerated
            ) {
                NID_ORIGIN_CODE_CUSTOMER
            } else {
                NID_ORIGIN_CODE_NID
            }
        } else {
            NID_ORIGIN_CODE_FAIL
        }
        return SessionIDOriginResult(origin, originCode, sessionID)
    }

    @Synchronized
    fun pauseCollection() {
        isSDKStarted = false
        if (pauseCollectionJob == null ||
            pauseCollectionJob?.isCancelled == true ||
            pauseCollectionJob?.isCompleted == true) {
            pauseCollectionJob = CoroutineScope(Dispatchers.IO).launch {
                nidJobServiceManager.sendEventsNow(true)
                nidJobServiceManager.stopJob()
                saveIntegrationHealthEvents()
            }
        }
    }

    @Synchronized
    fun resumeCollection() {
        if (pauseCollectionJob?.isCompleted == true ||
            pauseCollectionJob?.isCancelled == true ||
            pauseCollectionJob == null) {
            resumeCollectionCompletion()
        } else {
            pauseCollectionJob?.invokeOnCompletion {
                resumeCollectionCompletion()
            }
        }
    }

    private fun resumeCollectionCompletion() {
        isSDKStarted = true
        application?.let {
            if (!nidJobServiceManager.isSetup) {
                nidJobServiceManager.startJob(it, clientKey)
            } else {
                nidJobServiceManager.restart()
            }
        }
    }

    fun stopSession(): Boolean {
        dataStore.saveEvent(
            NIDEventModel(
                type = CLOSE_SESSION, ct = "SDK_EVENT", ts = System.currentTimeMillis()
            )
        )

        pauseCollection()
        clearSessionVariables()

        return true
    }
}