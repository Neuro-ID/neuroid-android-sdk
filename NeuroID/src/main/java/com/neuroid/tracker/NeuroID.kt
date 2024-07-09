package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.ATTEMPTED_LOGIN
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.CLOSE_SESSION
import com.neuroid.tracker.events.FORM_SUBMIT
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.events.FULL_BUFFER
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.events.SET_LINKED_SITE
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.extensions.captureIntegrationHealthEvent
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.ConfigService
import com.neuroid.tracker.service.LocationService
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.service.NIDConfigService
import com.neuroid.tracker.service.NIDHttpService
import com.neuroid.tracker.service.NIDIdentifierService
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDNetworkListener
import com.neuroid.tracker.service.NIDSamplingService
import com.neuroid.tracker.service.NIDSessionService
import com.neuroid.tracker.service.NIDValidationService
import com.neuroid.tracker.service.getSendingService
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDDataStoreManagerImp
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.utils.RandomGenerator
import com.neuroid.tracker.utils.VersionChecker
import com.neuroid.tracker.utils.getGUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.Calendar

class NeuroID
    private constructor(
        internal var application: Application?,
        internal var clientKey: String,
        internal val isAdvancedDevice: Boolean,
        serverEnvironment: String = PRODUCTION,
    ) : NeuroIDPublic {
        @Volatile internal var pauseCollectionJob: Job? = null // internal only for testing purposes

        private var firstTime = true
        internal var sessionID = ""
        internal var clientID = ""
        internal var userID = ""
        internal var linkedSiteID: String? = null

        internal var registeredUserID = ""
        internal var timestamp: Long = 0L
        internal val excludedTestIDList = arrayListOf<String>()

        internal var forceStart: Boolean = false

        internal var metaData: NIDMetaData? = null

        internal var verifyIntegrationHealth: Boolean = false
        internal var debugIntegrationHealthEvents: MutableList<NIDEventModel> =
            mutableListOf<NIDEventModel>()

        internal var isRN = false

        // Dependency Injections
        internal var dispatcher: CoroutineDispatcher = Dispatchers.IO
        internal var randomGenerator = RandomGenerator()
        internal var logger: NIDLogWrapper = NIDLogWrapper()
        internal var configService: ConfigService
        internal var dataStore: NIDDataStoreManager
        internal var registrationIdentificationHelper: RegistrationIdentificationHelper
        internal var nidActivityCallbacks: ActivityCallbacks
        internal var samplingService: NIDSamplingService
        internal val httpService: NIDHttpService
        internal val validationService: NIDValidationService = NIDValidationService(logger)
        internal var identifierService: NIDIdentifierService

        internal lateinit var sessionService: NIDSessionService
        internal lateinit var nidJobServiceManager: NIDJobServiceManager
        internal lateinit var nidCallActivityListener: NIDCallActivityListener
        internal lateinit var locationService: LocationService

        internal var clipboardManager: ClipboardManager? = null
        internal var networkConnectionType = "unknown"

        internal var lowMemory: Boolean = false
        internal var isConnected = false

        init {
            when (serverEnvironment) {
                PRODSCRIPT_DEVCOLLECTION -> {
                    endpoint = Constants.devEndpoint.displayName
                    scriptEndpoint = Constants.productionScriptsEndpoint.displayName
                }
                DEVELOPMENT -> {
                    endpoint = Constants.devEndpoint.displayName
                    scriptEndpoint = Constants.devScriptsEndpoint.displayName
                }
                else -> {
                    endpoint = Constants.productionEndpoint.displayName
                    scriptEndpoint = Constants.productionScriptsEndpoint.displayName
                }
            }

            // TO-DO - If invalid key passed we should be exiting
            if (!validationService.validateClientKey(clientKey)) {
                logger.e(msg = "Invalid Client Key")
                clientKey = ""
            } else {
                environment =
                    if (clientKey.contains("_live_")) {
                        "LIVE"
                    } else {
                        "TEST"
                    }
            }

            // We have to have two different retrofit instances because it requires a
            // `base_url` to work off of and our Collection and Config endpoints are
            // different
            httpService =
                NIDHttpService(
                    collectionEndpoint = endpoint,
                    configEndpoint = scriptEndpoint,
                    logger = logger,
                    // We can't use the config value because it hasn't been called.
                    // Might have to recreate once config is retrieved
                    collectionTimeout = 10,
                    configTimeout = 10,
                )

            configService = NIDConfigService(dispatcher, logger, this, httpService, validationService)
            samplingService = NIDSamplingService(logger, randomGenerator, configService)
            dataStore = NIDDataStoreManagerImp(logger, configService)

            identifierService =
                NIDIdentifierService(
                    logger,
                    neuroID = this,
                    validationService,
                )

            registrationIdentificationHelper = RegistrationIdentificationHelper(this, logger)
            nidActivityCallbacks = ActivityCallbacks(this, logger, registrationIdentificationHelper)

            application?.let {
                sessionService =
                    NIDSessionService(
                        logger,
                        this,
                        configService,
                        samplingService,
                        NIDSharedPrefsDefaults(it),
                        identifierService,
                        validationService,
                    )

                locationService = LocationService()
                metaData =
                    NIDMetaData(
                        it.applicationContext,
                    )

                nidJobServiceManager =
                    NIDJobServiceManager(
                        this,
                        dataStore,
                        getSendingService(
                            httpService,
                            it,
                        ),
                        logger,
                        configService,
                    )

                nidCallActivityListener = NIDCallActivityListener(this, VersionChecker())

                // get connectivity info on startup
                // register network listener here. >=API24 will not receive if registered
                // in the manifest so we do it here for full compatibility
                val connectivityManager =
                    it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val info = connectivityManager.activeNetworkInfo
                val isConnectingOrConnected = info?.isConnectedOrConnecting()
                if (isConnectingOrConnected != null) {
                    isConnected = isConnectingOrConnected
                }
                networkConnectionType = this.getNetworkType(it)

                application?.registerReceiver(
                    NIDNetworkListener(
                        connectivityManager,
                        this,
                        Dispatchers.IO,
                    ),
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                )
            }
        }

        /**
         * Function to retrieve the current network type (wifi, cell, eth, unknown)
         */
        internal fun getNetworkType(context: Context): String {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                activeNetwork?.let {
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(it)
                    networkCapabilities?.let { capabilities ->
                        return when {
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                            else -> "unknown"
                        }
                    }
                }
            } else {
                // For devices below API level 23
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo?.let {
                    return when (it.type) {
                        ConnectivityManager.TYPE_WIFI -> "wifi"
                        ConnectivityManager.TYPE_MOBILE -> "cell"
                        ConnectivityManager.TYPE_ETHERNET -> "eth"
                        else -> "unknown"
                    }
                }
            }
            return "noNetwork"
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

        data class Builder(
            val application: Application? = null,
            val clientKey: String = "",
            val isAdvancedDevice: Boolean = false,
            val serverEnvironment: String = PRODUCTION,
        ) {
            fun build() {
                val neuroID =
                    NeuroID(
                        application,
                        clientKey,
                        isAdvancedDevice,
                        serverEnvironment,
                    )
                neuroID.setupCallbacks()
                setNeuroIDInstance(neuroID)
            }
        }

        companion object {
            const val PRODUCTION = "production"
            const val DEVELOPMENT = "development"
            const val PRODSCRIPT_DEVCOLLECTION = "prodscriptdevcollection"

            // public exposed variable to determine if logs should show see
            //  `enableLogging`
            var showLogs: Boolean = true

            // Internal accessible property to allow internal get/set
            @Suppress("ktlint:standard:backing-property-naming")
            internal var _isSDKStarted: Boolean = false

            // public exposed variable for customers to see but NOT write to
            val isSDKStarted: Boolean
                get() = _isSDKStarted

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
            internal var scriptEndpoint = Constants.productionScriptsEndpoint.displayName
            private var singleton: NeuroID? = null

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
                nidJobServiceManager.setTestEventSender(
                    getSendingService(
                        NIDHttpService(
                            collectionEndpoint = endpoint,
                            configEndpoint = scriptEndpoint,
                            logger = logger,
                            // We can't use the config value because it hasn't been called.
                            // Might have to recreate once config is retrieved
                            collectionTimeout = 10,
                            configTimeout = 10,
                        ),
                        it,
                    ),
                )
            }
        }

        @VisibleForTesting
        override fun setTestingNeuroIDDevURL() {
            endpoint = Constants.devEndpoint.displayName
            scriptEndpoint = Constants.devScriptsEndpoint.displayName

            application?.let {
                nidJobServiceManager.setTestEventSender(
                    getSendingService(
                        httpService =
                            NIDHttpService(
                                collectionEndpoint = endpoint,
                                configEndpoint = scriptEndpoint,
                                logger = logger,
                                // We can't use the config value because it hasn't been called.
                                // Might have to recreate once config is retrieved
                                collectionTimeout = 10,
                                configTimeout = 10,
                            ),
                        it,
                    ),
                )
            }
        }

        override fun attemptedLogin(attemptedRegisteredUserId: String?): Boolean {
            try {
                attemptedRegisteredUserId?.let {
                    if (validationService.validateUserID(attemptedRegisteredUserId)) {
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
        internal fun checkThenCaptureAdvancedDevice(shouldCapture: Boolean = isAdvancedDevice) {
            val packageName = "com.neuroid.tracker.extensions"
            val methodName = "captureAdvancedDevice"
            val extensionName = ".AdvancedDeviceExtensionKt"
            try {
                val extensionFunctions = Class.forName(packageName + extensionName)
                val method =
                    extensionFunctions.getDeclaredMethod(
                        methodName,
                        NeuroID::class.java,
                        Boolean::class.java,
                    )
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

        override fun setScreenName(screen: String): Boolean {
            if (!isSDKStarted) {
                logger.e(msg = "NeuroID SDK is not started")
                return false
            }

            screenName = screen.replace("\\s".toRegex(), "%20")
            sessionService.createMobileMetadata()

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

        override fun start(completion: (Boolean) -> Unit) =
            sessionService.start(siteID = null) {
                completion(it)
            }

        override fun stop(): Boolean {
            return sessionService.stop()
        }

        internal fun resetClientId() {
            application?.let {
                val sharedDefaults = NIDSharedPrefsDefaults(it)
                clientID = sharedDefaults.resetClientID()
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

        override fun setIsRN() {
            this.isRN = true
        }

        override fun enableLogging(enable: Boolean) {
            showLogs = enable
        }

        override fun getSDKVersion() = NIDVersion.getSDKVersion()

        // Identifier Commands
        @Deprecated("Replaced with getUserID", ReplaceWith("getUserID()"))
        override fun getUserId() = identifierService.getUserID()

        override fun getUserID() = identifierService.getUserID()

        override fun setUserID(userID: String): Boolean {
            return identifierService.setUserID(userID, true)
        }

        override fun getRegisteredUserID() = identifierService.getRegisteredUserID()

        override fun setRegisteredUserID(registeredUserID: String): Boolean {
            return identifierService.setRegisteredUserID(registeredUserID)
        }

        // new Session Commands
        override fun startSession(
            sessionID: String?,
            completion: (SessionStartResult) -> Unit,
        ) = sessionService.startSession(null, sessionID) {
            completion(it)
        }

        @Synchronized
        override fun pauseCollection() {
            sessionService.pauseCollection(true)
        }

        @Synchronized
        override fun resumeCollection() {
            sessionService.resumeCollection()
        }

        override fun stopSession(): Boolean {
            return sessionService.stopSession()
        }

        /**
         * ported from the iOS implementation
         */
        override fun startAppFlow(
            siteID: String,
            userID: String?,
            completion: (SessionStartResult) -> Unit,
        ) {
            sessionService.startAppFlow(
                siteID,
                userID,
            ) {
                completion(it)
            }
        }

        override fun setVariable(
            key: String,
            value: String,
        ) {
            captureEvent(
                type = SET_VARIABLE,
                key = key,
                v = value,
            )
        }

        internal fun addLinkedSiteID(siteID: String) {
            if (!validationService.validateSiteID(siteID)) {
                captureEvent(
                    type = LOG,
                    level = "ERROR",
                    m = "Invalid Linked SiteID $siteID, failed to set",
                )

                return
            }

            linkedSiteID = siteID

            // capture linkedSiteEvent for MIHR - not relevant for collection
            captureEvent(type = SET_LINKED_SITE, v = siteID)
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
            isWifi: Boolean? = null,
            isConnected: Boolean? = null,
            cp: String? = null,
            l: Long? = null,
        ) {
            if (!queuedEvent && (!isSDKStarted || nidJobServiceManager.isStopped())) {
                return
            }

            if (excludedTestIDList.any { it == tgs || it == tg?.get("tgs") }) {
                return
            }

            if (!samplingService.isSessionFlowSampled()) {
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
                    isWifi,
                    isConnected,
                    cp,
                    l,
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
