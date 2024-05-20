package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
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
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.LocationService
import com.neuroid.tracker.service.NIDApiService
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.service.NIDConfigurationService
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDNetworkListener
import com.neuroid.tracker.service.OnRemoteConfigReceivedListener
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
import com.neuroid.tracker.utils.getRetroFitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

class NeuroID
    private constructor(
        internal var application: Application?,
        internal var clientKey: String,
        serverEnvironment: String = PRODUCTION
    ): NeuroIDPublic {
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
        internal var locationService: LocationService? = null

        internal var isAdvancedDevice = false

        init {
            when (serverEnvironment) {
                DEVELOPMENT -> {
                    endpoint = "${Constants.devEndpoint.displayName}"
                    scriptEndpoint = "${Constants.devScriptsEndpoint.displayName}"
                }
                else -> {
                    endpoint = "${Constants.productionEndpoint.displayName}"
                    scriptEndpoint = "${Constants.productionScriptsEndpoint.displayName}"
                }
            }

            setRemoteConfig()

            dataStore = NIDDataStoreManagerImp(logger)
            registrationIdentificationHelper = RegistrationIdentificationHelper(this, logger)
            nidActivityCallbacks = ActivityCallbacks(this, logger, registrationIdentificationHelper)
            application?.let {
                locationService = LocationService()
                metaData = NIDMetaData(
                    it.applicationContext,
                )

                nidJobServiceManager =
                    NIDJobServiceManager(
                        this,
                        dataStore,
                        getSendingService(endpoint, logger, it),
                        logger,
                    )
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
                    NIDNetworkListener(connectivityManager, dataStore, this, Dispatchers.IO),
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                )
            }

            // set call activity listener
            if (nidSDKConfig.callInProgress) {
                nidCallActivityListener = NIDCallActivityListener(dataStore, VersionChecker())
                this.getApplicationContext()
                    ?.let { nidCallActivityListener?.setCallActivityListener(it) }
            }
        }

        private fun setRemoteConfig() = runBlocking {
            val deferred = CoroutineScope(Dispatchers.IO).async {
                NIDConfigurationService(
                    getRetroFitInstance(scriptEndpoint, logger, NIDApiService::class.java),
                    object : OnRemoteConfigReceivedListener {
                        override fun onRemoteConfigReceived(remoteConfig: NIDRemoteConfig) {
                            nidSDKConfig = remoteConfig
                            logger.e("init", "remoteConfig: $remoteConfig")
                        }

                        override fun onRemoteConfigReceivedFailed(errorMessage: String) {
                            logger.e(
                                "init", "error getting remote config: $errorMessage"
                            )
                        }
                    }, clientKey
                )
            }
            deferred.await()
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

        data class Builder(val application: Application? = null,
                           val clientKey: String = "",
                           val serverEnvironment: String = PRODUCTION) {
            fun build() {
                val neuroID = NeuroID(application, clientKey, serverEnvironment)
                neuroID.setupCallbacks()
                setNeuroIDInstance(neuroID)
            }
        }

        companion object {
            const val PRODUCTION = "production"
            const val DEVELOPMENT = "development"

            var showLogs: Boolean = true
            var isSDKStarted = false

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

            internal var endpoint = "${Constants.productionEndpoint.displayName}"
            internal var scriptEndpoint = "${Constants.productionScriptsEndpoint.displayName}"
            private var singleton: NeuroID? = null

            // configuration state
            internal var nidSDKConfig = NIDRemoteConfig()

            internal fun setNeuroIDInstance(neuroID: NeuroID) {
                if (singleton == null) {
                    singleton = neuroID
                    singleton?.setupCallbacks()
                }
            }

            fun getInstance(): NeuroIDPublic? = singleton

            internal fun getInternalInstance() = singleton
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
        override fun setTestURL(newEndpoint: String) {
            endpoint = newEndpoint
            scriptEndpoint = Constants.devScriptsEndpoint.displayName

            application?.let {
                nidJobServiceManager.setTestEventSender(getSendingService(endpoint, logger, it))
            }
        }

        @VisibleForTesting
        override fun setTestingNeuroIDDevURL() {
            endpoint = Constants.devEndpoint.displayName
            scriptEndpoint = Constants.devScriptsEndpoint.displayName

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

        override fun setRegisteredUserID(registeredUserId: String): Boolean {
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

        override fun attemptedLogin(attemptedRegisteredUserId: String?): Boolean {
            try {
                attemptedRegisteredUserId?.let {
                    if (validateUserId(attemptedRegisteredUserId)) {
                        captureEvent(type = ATTEMPTED_LOGIN, uid = attemptedRegisteredUserId)
                        return true
                    }
                }
                captureEvent(type = ATTEMPTED_LOGIN, uid = "scrubbed-id-failed-validation")
                return true
            } catch (exception: Exception) {
                logger.e(msg = "exception in attemptedLogin() ${exception.message}")
                return false
            }
        }

        /**
         * Execute the captureAdvancedDevice() method without knowing if the class that holds
         * it exists. Required because we moved the ADV setup to the start()/startSession()
         * methods.
         *
         * This method will look for the AdvancedDeviceExtension file, create a class from it, find
         * the method captureAdvancedDevice() within the extension class and call it with the
         * shouldCapture boolean if the advanced lib is used. If this is called in the non advanced lib,
         * the method will look for the AdvancedDeviceExtension file, not find it and fail the
         * Class.forName() call which will throw a ClassNotFoundException because it doesn't exist.
         * We catch this exception and log it to logcat.
         *
         * We must handle all exceptions that this method may
         * throw because of the undependable nature of reflection. We cannot afford to throw any
         * exception to the host app causing a crash because of this .
         */
        internal fun checkThenCaptureAdvancedDevice(shouldCapture:Boolean = isAdvancedDevice) {
            val packageName = "com.neuroid.tracker.extensions"
            val methodName = "captureAdvancedDevice"
            val extensionName = ".AdvancedDeviceExtensionKt"
            try {
                val extensionFunctions = Class.forName(packageName + extensionName)
                val method = extensionFunctions.getDeclaredMethod(methodName, NeuroID::class.java,
                    Boolean::class.java)
                if (method != null) {
                    method.isAccessible = true
                    // Invoke the method
                    method.invoke(null, this, shouldCapture)
                    logger.d(msg = "Method $methodName invoked successfully")
                } else {
                    logger.d(msg = "No $methodName method found")
                }
            } catch (e: ClassNotFoundException) {
                logger.e(msg = "Class $packageName$extensionName not found")
            } catch (e: Exception) {
                logger.e(msg = "Failed to perform $methodName: with error: ${e.message}")
            }
        }

        override fun setUserID(userID: String): Boolean {
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
        override fun getUserId() = getUserID()

        override fun getUserID() = userID

        override fun getRegisteredUserID() = registeredUserID

        override fun setScreenName(screen: String): Boolean {
            if (!isSDKStarted) {
                logger.e(msg = "NeuroID SDK is not started")
                return false
            }

            screenName = screen.replace("\\s".toRegex(), "%20")
            createMobileMetadata()

            return true
        }

        override fun getScreenName(): String = screenName

        @Synchronized
        override fun excludeViewByTestID(id: String) {
            if (excludedTestIDList.none { it == id }) {
                excludedTestIDList.add(id)
            }
        }

        @Deprecated("setEnvironment is deprecated and no longer required")
        override fun setEnvironment(environment: String) {
            logger.i(msg = "**** NOTE: setEnvironment METHOD IS DEPRECATED")
        }

        @Deprecated("setEnvironmentProduction is deprecated and no longer required")
        override fun setEnvironmentProduction(prod: Boolean) {
            logger.i(msg = "**** NOTE: setEnvironmentProduction METHOD IS DEPRECATED")
        }

        override fun getEnvironment(): String = environment

        @Deprecated("setSiteId is deprecated and no longer required")
        override fun setSiteId(siteId: String) {
            logger.i(msg = "**** NOTE: setSiteId METHOD IS DEPRECATED")

            siteID = siteId
        }

        @Deprecated("getSiteId is deprecated")
        internal fun getSiteId(): String {
            logger.i(msg = "**** NOTE: getSiteId METHOD IS DEPRECATED")
            return ""
        }

        @Deprecated("Replaced with getSessionID", ReplaceWith("getSessionID()"))
        override fun getSessionId(): String {
            return getSessionID()
        }

        override fun getSessionID(): String = sessionID

        @Deprecated("Replaced with getClientID", ReplaceWith("getClientID()"))
        override fun getClientId(): String {
            return getClientID()
        }

        override fun getClientID(): String = clientID

        internal fun shouldForceStart(): Boolean {
            return forceStart
        }

        override fun registerPageTargets(activity: Activity) {
            this.forceStart = true
            nidActivityCallbacks.forceStart(activity)
        }

        internal fun getTabId(): String = rndmId

        internal fun getFirstTS(): Long = timestamp

        @Deprecated("formSubmit is deprecated and no longer required")
        override fun formSubmit() {
            logger.i(msg = "**** NOTE: formSubmit METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT)
            saveIntegrationHealthEvents()
        }

        @Deprecated("formSubmitSuccess is deprecated and no longer required")
        override fun formSubmitSuccess() {
            logger.i(msg = "**** NOTE: formSubmitSuccess METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT_SUCCESS)
            saveIntegrationHealthEvents()
        }

        @Deprecated("formSubmitFailure is deprecated and no longer required")
        override fun formSubmitFailure() {
            logger.i(msg = "**** NOTE: formSubmitFailure METHOD IS DEPRECATED")

            captureEvent(type = FORM_SUBMIT_FAILURE)
            saveIntegrationHealthEvents()
        }

        override fun start(): Boolean {
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

            this.getApplicationContext()?.let {
                if (nidSDKConfig.callInProgress) {
                    nidCallActivityListener?.setCallActivityListener(it)
                }
                if (nidSDKConfig.geoLocation) {
                    locationService?.setupLocationCoroutine(it.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                }
            }
            checkThenCaptureAdvancedDevice()
            return true
        }

        override fun stop(): Boolean {
            pauseCollection()
            nidCallActivityListener?.unregisterCallActivityListener(this.getApplicationContext())

            locationService?.let {
                it.shutdownLocationCoroutine(getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            }

            return true
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

        override fun isStopped() = !isSDKStarted

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

        /** Provide public access to application context for other internal NID functions */
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
                // get updated GPS location if possible
                metaData?.getLastKnownLocation(it, nidSDKConfig.geoLocation, locationService)
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

        override fun setIsRN() {
            this.isRN = true
        }

        override fun enableLogging(enable: Boolean) {
            showLogs = enable
        }

        override fun getSDKVersion() = NIDVersion.getSDKVersion()

        // new Session Commands
        fun clearSessionVariables() {
            userID = ""
            registeredUserID = ""
        }

        override fun startSession(sessionID: String?): SessionStartResult {
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

            if (nidSDKConfig.callInProgress) {
                this.getApplicationContext()
                    ?.let { nidCallActivityListener?.setCallActivityListener(it) }
            }

            // we need to set finalSessionID with the set random user id
            // if a sessionID was not passed in
            finalSessionID = getUserID()

            if (nidSDKConfig.geoLocation) {
                locationService?.let {
                    it.setupLocationCoroutine(getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                }
            }
            checkThenCaptureAdvancedDevice()
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
        override fun pauseCollection() {
            pauseCollection(true)
        }

        @Synchronized
        internal fun pauseCollection(flushEvents: Boolean) {
            isSDKStarted = false
            if (pauseCollectionJob == null ||
                pauseCollectionJob?.isCancelled == true ||
                pauseCollectionJob?.isCompleted == true
            ) {
                pauseCollectionJob =
                    CoroutineScope(Dispatchers.IO).launch {
                        nidJobServiceManager.sendEvents(flushEvents)
                        nidJobServiceManager.stopJob()
                        saveIntegrationHealthEvents()
                    }
            }
            locationService?.let {
                it.shutdownLocationCoroutine(getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            }
        }

        @Synchronized
        override fun resumeCollection() {
            // Don't allow resume to be called if SDK has not been started
            if (userID.isEmpty() && !isSDKStarted) {
                return
            }
            if (pauseCollectionJob?.isCompleted == true ||
                pauseCollectionJob?.isCancelled == true ||
                pauseCollectionJob == null
            ) {
                resumeCollectionCompletion()
            } else {
                pauseCollectionJob?.invokeOnCompletion { resumeCollectionCompletion() }
            }

            if (nidSDKConfig.geoLocation) {
                locationService?.let {
                    it.setupLocationCoroutine(getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
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

        override fun stopSession(): Boolean {
            captureEvent(type = CLOSE_SESSION, ct = "SDK_EVENT")

            pauseCollection()
            clearSessionVariables()
            nidCallActivityListener?.unregisterCallActivityListener(this.getApplicationContext())

            locationService?.let {
                it.shutdownLocationCoroutine(getApplicationContext()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            }

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
            // had to change to Calendar for tests. Cannot mock System methods.
            ts: Long = Calendar.getInstance().timeInMillis,
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
            if (lowMemory || fullBuffer) {
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
                    ioDispatcher.launch { nidJobServiceManager.sendEvents() }
                }
                CLOSE_SESSION -> {
                    ioDispatcher.launch { nidJobServiceManager.sendEvents(true) }
                }
            }
        }
    }
