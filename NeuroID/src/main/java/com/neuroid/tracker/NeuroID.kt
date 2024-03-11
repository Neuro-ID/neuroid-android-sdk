package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.view.View
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.extensions.captureIntegrationHealthEvent
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.extensions.startIntegrationHealthCheck
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDNetworkListener
import com.neuroid.tracker.service.getSendingService
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDDataStoreManagerImp
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.NIDSingletonIDs
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.utils.VersionChecker
import com.neuroid.tracker.utils.generateUniqueHexId
import com.neuroid.tracker.utils.getGUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NeuroID
    private constructor(
        internal var application: Application?,
        internal var clientKey: String,
    ) {
        @Volatile internal var pauseCollectionJob: Job? = null // internal only for testing purposes
        private val ioDispatcher: CoroutineScope = CoroutineScope(Dispatchers.IO)

        private var firstTime = true
        internal var sessionID = ""
        internal var clientID = ""
        internal var userID = ""
        internal var registeredUserID = ""
        internal var timestamp: Long = 0L
        internal val excludedTestIDList = arrayListOf<String>()

        internal var forceStart: Boolean = false

        private var metaData: NIDMetaData? = null

        internal var verifyIntegrationHealth: Boolean = false
        internal var debugIntegrationHealthEvents: MutableList<NIDEventModel> =
            mutableListOf<NIDEventModel>()

        internal var isRN = false

        // Dependency Injections
        internal var logger: NIDLogWrapper = NIDLogWrapper()
        internal var dataStore: NIDDataStoreManager
        internal var registrationIdentificationHelper: RegistrationIdentificationHelper
        internal var nidActivityCallbacks: ActivityCallbacks
        internal lateinit var nidJobServiceManager: NIDJobServiceManager

        internal var clipboardManager: ClipboardManager? = null

        internal var nidCallActivityListener: NIDCallActivityListener? = null
        internal var lowMemory: Boolean = false
        internal var isConnected = false

        init {
            dataStore = NIDDataStoreManagerImp(logger)
            registrationIdentificationHelper = RegistrationIdentificationHelper(this, logger)
            nidActivityCallbacks =
                ActivityCallbacks(this, logger, registrationIdentificationHelper)

            application?.let {
                metaData = NIDMetaData(it.applicationContext)

                nidJobServiceManager =
                    NIDJobServiceManager(this, dataStore, getSendingService(endpoint, logger, it), logger)
            }

            if (!validateClientKey(clientKey)) {
                logger.e(msg = "Invalid Client Key")
                clientKey = ""
            } else {
                if (clientKey.contains("_live_")) {
                    environment = "LIVE"
                } else {
                    environment = "TEST"
                }
            }

            // register network listener here. >=API24 will not receive if registered
            // in the manifest so we do it here for full compatibility
            application?.let {
                val connectivityManager =
                    it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                application?.registerReceiver(
                    NIDNetworkListener(
                        connectivityManager,
                        dataStore,
                        this,
                    ),
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                )
            }

            // set call activity listener
            nidCallActivityListener = NIDCallActivityListener(dataStore, VersionChecker())
            this.getApplicationContext()?.let { nidCallActivityListener?.setCallActivityListener(it) }
        }

        @Synchronized
        private fun setupCallbacks() {
            if (firstTime) {
                firstTime = false
                application?.let {
                    it.registerActivityLifecycleCallbacks(nidActivityCallbacks)
                    NIDTimerActive.initTimer()
                }
            }
        }

        data class Builder(var application: Application? = null, var clientKey: String = "") {
            fun build() = NeuroID(application, clientKey)
        }

        companion object {
            var showLogs: Boolean = true
            var isSDKStarted = false

            internal val GYRO_SAMPLE_INTERVAL = 200L
            internal val captureGyroCadence = true

            @get:Synchronized @set:Synchronized
            internal var screenName = ""

            @get:Synchronized @set:Synchronized
            internal var screenActivityName = ""

            @get:Synchronized @set:Synchronized
            internal var screenFragName = ""

            internal var environment = ""
            internal var siteID = ""
            internal var rndmId = "mobile"
            internal var firstScreenName = ""
            internal var isConnected = false

            internal var registeredViews: MutableSet<String> = mutableSetOf()

            internal var endpoint = Constants.productionEndpoint.displayName
            private var singleton: NeuroID? = null

            @JvmStatic
            @Deprecated(
                "setNeuroIdInstance has been renamed to setNeuroIDInstance",
                ReplaceWith("NeuroID.setNeuroIDInstance()"),
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

        internal fun setLoggerInstance(newLogger: NIDLogWrapper) {
            logger = newLogger
        }

        internal fun setDataStoreInstance(store: NIDDataStoreManager) {
            dataStore = store
        }

        internal fun setClipboardManagerInstance(cm: ClipboardManager) {
            clipboardManager = cm
        }

        internal fun getClipboardManagerInstance(): ClipboardManager? {
            if (clipboardManager == null) {
                return getApplicationContext()?.getSystemService(Context.CLIPBOARD_SERVICE) as
                    ClipboardManager
            }
            return clipboardManager
        }

        internal fun setNIDActivityCallbackInstance(callback: ActivityCallbacks) {
            nidActivityCallbacks = callback
        }

        internal fun setNIDJobServiceManager(serviceManager: NIDJobServiceManager) {
            nidJobServiceManager = serviceManager
        }

        @VisibleForTesting
        fun setTestURL(newEndpoint: String) {
            endpoint = newEndpoint

            application?.let {
                nidJobServiceManager.setTestEventSender(getSendingService(endpoint, logger, it))
            }
        }

        @VisibleForTesting
        fun setTestingNeuroIDDevURL() {
            endpoint = Constants.devEndpoint.displayName

            application?.let {
                nidJobServiceManager.setTestEventSender(getSendingService(endpoint, logger, it))
            }
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
                logger.e(msg = "Invalid UserID")
                return false
            }

            return true
        }

        fun setRegisteredUserID(registeredUserId: String): Boolean {
            val validID =
                setGenericUserID(
                    SET_REGISTERED_USER_ID,
                    registeredUserId,
                )

            if (!validID) {
                return false
            }

            this.registeredUserID = registeredUserId
            return true
        }

        fun setUserID(userID: String): Boolean {
            return setUserID(userID, true)
        }

        private fun setUserID(
            userId: String,
            userGenerated: Boolean,
        ): Boolean {
            val validID = setGenericUserID(SET_USER_ID, userId, userGenerated)

            if (!validID) {
                return false
            }

            this.userID = userId
            return true
        }

        internal fun setGenericUserID(
            type: String,
            genericUserId: String,
            userGenerated: Boolean = true,
        ): Boolean {
            try {
                val validID = validateUserId(genericUserId)
                val originRes =
                    getOriginResult(genericUserId, validID = validID, userGenerated = userGenerated)
                sendOriginEvent(originRes)

                if (!validID) {
                    return false
                }

                if (isSDKStarted) {
                    captureEvent(
                        type = type,
                        uid = genericUserId,
                    )
                } else {
                    captureEvent(
                        queuedEvent = true,
                        type = type,
                        uid = genericUserId,
                    )
                }

                return true
            } catch (exception: Exception) {
                logger.e(msg = "failure processing user id! $type, $genericUserId $exception")
                return false
            }
        }

        @Deprecated("Replaced with getUserID", ReplaceWith("getUserID()"))
        fun getUserId() = userID

        fun getUserID() = userID

        fun getRegisteredUserID() = registeredUserID

        fun setScreenName(screen: String): Boolean {
            if (!isSDKStarted) {
                logger.e(msg = "NeuroID SDK is not started")
                return false
            }

            screenName = screen.replace("\\s".toRegex(), "%20")
            createMobileMetadata()

            return true
        }

        fun getScreenName(): String = screenName

        @Synchronized
        fun excludeViewByTestID(id: String) {
            if (excludedTestIDList.none { it == id }) {
                excludedTestIDList.add(id)
            }
        }

        @Deprecated("setEnvironment is deprecated and no longer required")
        fun setEnvironment(environment: String) {
            logger.i(msg = "**** NOTE: setEnvironment METHOD IS DEPRECATED")
        }

        @Deprecated("setEnvironmentProduction is deprecated and no longer required")
        fun setEnvironmentProduction(prod: Boolean) {
            logger.i(msg = "**** NOTE: setEnvironmentProduction METHOD IS DEPRECATED")
        }

        fun getEnvironment(): String = environment

        @Deprecated("getSiteId is deprecated and no longer required")
        fun setSiteId(siteId: String) {
            logger.i(msg = "**** NOTE: setSiteId METHOD IS DEPRECATED")

            siteID = siteId
        }

        @Deprecated("getSiteId is deprecated")
        internal fun getSiteId(): String {
            logger.i(msg = "**** NOTE: getSiteId METHOD IS DEPRECATED")
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

        internal fun shouldForceStart(): Boolean {
            return forceStart
        }

        fun registerPageTargets(activity: Activity) {
            this.forceStart = true
            nidActivityCallbacks.forceStart(activity)
        }

        internal fun getTabId(): String = rndmId

        internal fun getFirstTS(): Long = timestamp

        @Deprecated("formSubmit is deprecated and no longer required")
        fun formSubmit() {
            logger.i(msg = "**** NOTE: formSubmit METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT)
            saveIntegrationHealthEvents()
        }

        @Deprecated("formSubmitSuccess is deprecated and no longer required")
        fun formSubmitSuccess() {
            logger.i(msg = "**** NOTE: formSubmitSuccess METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT_SUCCESS)
            saveIntegrationHealthEvents()
        }

        @Deprecated("formSubmitFailure is deprecated and no longer required")
        fun formSubmitFailure() {
            logger.i(msg = "**** NOTE: formSubmitFailure METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT_FAILURE)
            saveIntegrationHealthEvents()
        }

        open fun start(): Boolean {
            if (clientKey == "") {
                logger.e(msg = "Missing Client Key - please call configure prior to calling start")
                return false
            }

            application?.let { nidJobServiceManager.startJob(it, clientKey) }
            isSDKStarted = true
            NIDSingletonIDs.retrieveOrCreateLocalSalt()

            CoroutineScope(Dispatchers.IO).launch {
                startIntegrationHealthCheck()
                createSession()
                saveIntegrationHealthEvents()
            }
            dataStore.saveAndClearAllQueuedEvents()

            this.getApplicationContext()?.let { nidCallActivityListener?.setCallActivityListener(it) }

            return true
        }

        fun stop() {
            pauseCollection()
            nidCallActivityListener?.unregisterCallActivityListener(this.getApplicationContext())
        }

        fun closeSession() {
            if (!isStopped()) {
                captureEvent(type = CLOSE_SESSION, ct = "SDK_EVENT")
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

        internal fun registerTarget(
            activity: Activity,
            view: View,
            addListener: Boolean,
        ) {
            registrationIdentificationHelper.identifySingleView(
                view,
                activity.getGUID(),
                true,
                addListener,
            )
        }

        /** Provide public access to application context for other intenral NID functions */
        internal fun getApplicationContext(): Context? {
            return this.application?.applicationContext
        }

        private fun createSession() {
            timestamp = System.currentTimeMillis()
            application?.let {
                val sharedDefaults = NIDSharedPrefsDefaults(it)
                sessionID = sharedDefaults.getNewSessionID()
                clientID = sharedDefaults.getClientId()

                captureEvent(
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
                    sw = NIDSharedPrefsDefaults.getDisplayWidth().toFloat(),
                    sh = NIDSharedPrefsDefaults.getDisplayHeight().toFloat(),
                    metadata = metaData,
                )

                createMobileMetadata()
            }
        }

        private fun createMobileMetadata() {
            application?.let {
                val sharedDefaults = NIDSharedPrefsDefaults(it)
                captureEvent(
                    type = MOBILE_METADATA_ANDROID,
                    sw = NIDSharedPrefsDefaults.getDisplayWidth().toFloat(),
                    sh = NIDSharedPrefsDefaults.getDisplayHeight().toFloat(),
                    metadata = metaData,
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
                    attrs = listOf(mapOf("isRN" to isRN)),
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
        }

        fun startSession(sessionID: String? = null): SessionStartResult {
            if (clientKey == "") {
                logger.e(msg = "Missing Client Key - please call configure prior to calling start")
                return SessionStartResult(false, "")
            }

            if (userID != "" || isSDKStarted) {
                stopSession()
            }

            var finalSessionID = sessionID ?: generateUniqueHexId()

            if (!setUserID(finalSessionID, sessionID != null)) {
                return SessionStartResult(false, "")
            }

            resumeCollection()

            isSDKStarted = true
            NIDSingletonIDs.retrieveOrCreateLocalSalt()

            CoroutineScope(Dispatchers.IO).launch {
                startIntegrationHealthCheck()
                createSession()
                saveIntegrationHealthEvents()
            }

            dataStore.saveAndClearAllQueuedEvents()

            this.getApplicationContext()?.let { nidCallActivityListener?.setCallActivityListener(it) }

            // we need to set finalSessionID with the set random user id
            // if a sessionID was not passed in
            finalSessionID = getUserID()

            return SessionStartResult(true, finalSessionID)
        }

        private fun sendOriginEvent(originResult: SessionIDOriginResult) {
            // sending these as individual items.
            captureEvent(
                queuedEvent = !isSDKStarted,
                type = SET_VARIABLE,
                key = "sessionIdCode",
                v = originResult.originCode,
            )

            captureEvent(
                queuedEvent = !isSDKStarted,
                type = SET_VARIABLE,
                key = "sessionIdSource",
                v = originResult.origin,
            )

            captureEvent(
                queuedEvent = !isSDKStarted,
                type = SET_VARIABLE,
                key = "sessionId",
                v = originResult.sessionID,
            )
        }

        internal fun getOriginResult(
            sessionID: String,
            validID: Boolean,
            userGenerated: Boolean,
        ): SessionIDOriginResult {
            var origin =
                if (userGenerated) {
                    NID_ORIGIN_CUSTOMER_SET
                } else {
                    NID_ORIGIN_NID_SET
                }

            var originCode =
                if (validID) {
                    if (userGenerated) {
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
                pauseCollectionJob?.isCompleted == true
            ) {
                pauseCollectionJob =
                    CoroutineScope(Dispatchers.IO).launch {
                        nidJobServiceManager.sendEvents(true)
                        nidJobServiceManager.stopJob()
                        saveIntegrationHealthEvents()
                    }
            }
        }

        @Synchronized
        fun resumeCollection() {
            if (pauseCollectionJob?.isCompleted == true ||
                pauseCollectionJob?.isCancelled == true ||
                pauseCollectionJob == null
            ) {
                resumeCollectionCompletion()
            } else {
                pauseCollectionJob?.invokeOnCompletion { resumeCollectionCompletion() }
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
            captureEvent(type = CLOSE_SESSION, ct = "SDK_EVENT")

            pauseCollection()
            clearSessionVariables()
            nidCallActivityListener?.unregisterCallActivityListener(this.getApplicationContext())
            return true
        }

    /*
        Every Event Saved should run through this function
        This function will verify that events should be saved to the datastore prior to making an event object
        Additionally it will send all events in the queue if the type is a special type (see end of function)
     */
        internal fun captureEvent(
            queuedEvent: Boolean = false,
            type: String,
            ts: Long = System.currentTimeMillis(),
            attrs: List<Map<String, Any>>? = null,
            tg: Map<String, Any>? = null,
            tgs: String? = null,
            touches: List<NIDTouchModel>? = null,
            key: String? = null,
            gyro: NIDSensorModel? = NIDSensorHelper.getGyroscopeInfo(),
            accel: NIDSensorModel? = NIDSensorHelper.getAccelerometerInfo(),
            v: String? = null,
            hv: String? = null,
            en: String? = null,
            etn: String? = null,
            ec: String? = null,
            et: String? = null,
            eid: String? = null,
            ct: String? = null,
            sm: Int? = null,
            pd: Int? = null,
            x: Float? = null,
            y: Float? = null,
            w: Int? = null,
            h: Int? = null,
            sw: Float? = null,
            sh: Float? = null,
            f: String? = null,
            lsid: String? = null,
            sid: String? = null,
            siteId: String? = null,
            cid: String? = null,
            did: String? = null,
            iid: String? = null,
            loc: String? = null,
            ua: String? = null,
            tzo: Int? = null,
            lng: String? = null,
            ce: Boolean? = null,
            je: Boolean? = null,
            ol: Boolean? = null,
            p: String? = null,
            dnt: Boolean? = null,
            tch: Boolean? = null,
            url: String? = null,
            ns: String? = null,
            jsl: List<String>? = null,
            jsv: String? = null,
            uid: String? = null,
            o: String? = null,
            rts: String? = null,
            metadata: NIDMetaData? = null,
            rid: String? = null,
            m: String? = null,
            level: String? = null,
            c: Boolean? = null,
            isConnected: Boolean? = null,
        ) {
            if (!queuedEvent && (!isSDKStarted || nidJobServiceManager.isStopped())) {
                return
            }

            if (excludedTestIDList.any { it == tgs || it == tg?.get("tgs") }) {
                return
            }

            val fullBuffer = dataStore.isFullBuffer()
            if (lowMemory || fullBuffer)
                {
                    logger.w("NeuroID", "Data store buffer ${FULL_BUFFER}, $type dropped")
                    return
                }

            val event =
                NIDEventModel(
                    type,
                    ts,
                    attrs,
                    tg,
                    tgs,
                    touches,
                    key,
                    gyro,
                    accel,
                    v,
                    hv,
                    en,
                    etn,
                    ec,
                    et,
                    eid,
                    ct,
                    sm,
                    pd,
                    x,
                    y,
                    w,
                    h,
                    sw,
                    sh,
                    f,
                    lsid,
                    sid,
                    siteId,
                    cid,
                    did,
                    iid,
                    loc,
                    ua,
                    tzo,
                    lng,
                    ce,
                    je,
                    ol,
                    p,
                    dnt,
                    tch,
                    url,
                    ns,
                    jsl,
                    jsv,
                    uid,
                    o,
                    rts,
                    metadata,
                    rid,
                    m,
                    level,
                    c,
                    isConnected,
                )

            if (queuedEvent) {
                dataStore.queueEvent(event)
            } else {
                dataStore.saveEvent(event)
            }

            event.log()
            captureIntegrationHealthEvent(event)

            when (type) {
                BLUR -> {
                    nidJobServiceManager.sendEvents()
                }
                CLOSE_SESSION -> {
                    nidJobServiceManager.sendEvents(true)
                }
            }
        }
    }
