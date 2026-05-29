package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.events.APPLICATION_METADATA
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.utils.NIDBuildConfigWrapper
import com.neuroid.tracker.utils.NIDVersion
import com.neuroid.tracker.models.NIDConfiguration
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.service.NIDSessionService
import com.neuroid.tracker.service.LocationService
import com.neuroid.tracker.service.getSendingService
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.getAppMetaData
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar

enum class TestLogLevel {
    DEBUG,
    INFO,
    ERROR,
    WARNING,
}

open class NeuroIDClassUnitTests {
    private var errorCount = 0
    private var infoCount = 0
    private var debugCount = 0
    private var warningCount = 0

    // datastoreMock vars
    private var storedEvents = mutableSetOf<NIDEventModel>()
    private var queuedEvents = mutableSetOf<NIDEventModel>()

    private fun assertLogMessage(
        type: TestLogLevel,
        expectedMessage: String,
        actualMessage: Any?,
    ) {
        if (actualMessage != "" && actualMessage != null) {
            assertEquals(expectedMessage, actualMessage)
        }

        when (type) {
            TestLogLevel.DEBUG -> debugCount += 1
            TestLogLevel.INFO -> infoCount += 1
            TestLogLevel.ERROR -> errorCount += 1
            TestLogLevel.WARNING -> warningCount += 1
        }

        return
    }

    // Helper Functions
    private fun setNeuroIDInstance() {
        // set NeuroID singleton to null, else the NeuroID already initialized error will occur and
        // fail test when NeuroID is built.
        NeuroID.setSingletonNull()
        NeuroID.Builder(null, "key_test_fake1234", false, NeuroID.DEVELOPMENT).build()
    }

    private fun setNeuroIDInstanceBuilderConfig() {
        // set NeuroID singleton to null, else the NeuroID already initialized error will occur and
        // fail test when NeuroID is built.
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration(
                "key_test_fake1234",
                false,
                "",
                true,
                ""
            )
        ).build()
    }

    private fun setNeuroIDMockedLogger(
        errorMessage: String = "",
        infoMessage: String = "",
        debugMessage: String = "",
        warningMessage: String = "",
    ) {
        val log = mockk<NIDLogWrapper>()

        every { log.e(any(), any()) } answers {
            val actualMessage = args[1]
            assertLogMessage(TestLogLevel.ERROR, errorMessage, actualMessage)

            // Return the result
            actualMessage
        }

        every { log.i(any(), any()) } answers {
            val actualMessage = args[1]
            assertLogMessage(TestLogLevel.INFO, infoMessage, actualMessage)

            // Return the result
            actualMessage
        }

        every { log.d(any(), any()) } answers {
            val actualMessage = args[1]
            assertLogMessage(TestLogLevel.DEBUG, debugMessage, actualMessage)

            // Return the result
            actualMessage
        }

        every { log.w(any(), any()) } answers {
            val actualMessage = args[1]
            assertLogMessage(TestLogLevel.WARNING, warningMessage, actualMessage)

            // Return the result
            actualMessage
        }

        NeuroID.getInternalInstance()?.setLoggerInstance(log)
    }

    fun setMockedDataStore(fullBuffer: Boolean = false): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } answers {
            storedEvents.add(args[0] as NIDEventModel)
            mockk<Job>()
        }

        every { dataStoreManager.queueEvent(any()) } answers {
            queuedEvents.add(args[0] as NIDEventModel)
        }

        every { dataStoreManager.saveAndClearAllQueuedEvents() } answers { queuedEvents.clear() }

        every { dataStoreManager.isFullBuffer() } returns fullBuffer

        NeuroID.getInternalInstance()?.setDataStoreInstance(dataStoreManager)

        return dataStoreManager
    }

    internal fun setMockedApplication() {
        val mockedApplication = mockk<Application>()

        every { mockedApplication.applicationContext } answers {
            mockk<Context>()
        }

        NeuroID.getInternalInstance()?.application = mockedApplication
    }

    private fun setMockedNIDJobServiceManager(isStopped: Boolean = true) {
        val mockedNIDJobServiceManager = mockk<NIDJobServiceManager>()

        every { mockedNIDJobServiceManager.startJob(any(), any()) } just runs
        every { mockedNIDJobServiceManager.isStopped() } returns isStopped
        every { mockedNIDJobServiceManager.stopJob() } just runs

        coEvery {
            mockedNIDJobServiceManager.sendEvents(any())
        } just runs

        NeuroID.getInternalInstance()?.setNIDJobServiceManager(mockedNIDJobServiceManager)
    }

    private fun mockCalendarTS() {
        mockkStatic(Calendar::class)
        every { Calendar.getInstance().timeInMillis } returns 0
    }

    private fun clearLogCounts() {
        errorCount = 0
        infoCount = 0
        debugCount = 0
        warningCount = 0
    }

    private fun getDeprecatedMessage(fnName: String): String {
        return "**** NOTE: $fnName METHOD IS DEPRECATED"
    }

    // testing helper functions
    private fun assertErrorCount(count: Int) {
        assertEquals(count, errorCount)
        clearLogCounts()
    }

    private fun assertInfoCount(count: Int) {
        assertEquals(count, infoCount)
        clearLogCounts()
    }

    private fun assertDebugCount(count: Int) {
        assertEquals(count, debugCount)
        clearLogCounts()
    }

    private fun assertWarningCount(count: Int) {
        assertEquals(count, warningCount)
        clearLogCounts()
    }

    fun unsetDefaultMockedLogger() {
        val log = mockk<NIDLogWrapper>()
        every { log.d(any(), any()) } just runs
        every { log.e(any(), any()) } just runs
        NeuroID.getInternalInstance()?.setLoggerInstance(log)
    }

    @Before
    fun setUp() {
        // setup instance and logging, use new BuilderConfig
        setNeuroIDInstanceBuilderConfig()
        NeuroID.getInternalInstance()?.application = null
        setNeuroIDMockedLogger()

        clearLogCounts()
        storedEvents.clear()
        queuedEvents.clear()
        NeuroID.getInternalInstance()?.excludedTestIDList?.clear()
    }

    @After
    fun tearDown() {
        assertEquals("Expected Log Error Count is Greater than 0", 0, errorCount)
        assertEquals("Expected Log Info Count is Greater than 0", 0, infoCount)
        assertEquals("Expected Log Debug Count is Greater than 0", 0, debugCount)
        assertEquals("Expected Log Warning Count is Greater than 0", 0, warningCount)

        NeuroID.getInternalInstance()?.userID = ""
        NeuroID.getInternalInstance()?.registeredUserID = ""
        NeuroID.getInternalInstance()?.linkedSiteID = ""

        unmockkAll()
    }

    // Function Tests

    //    setLoggerInstance - Used for mocking
    //    setDataStoreInstance - Used for mocking
    //    setNIDActivityCallbackInstance - Used for mocking
    //    setNIDJobServiceManager - Used for mocking

    //   setTestURL

    @Test
    fun test_ConfigOld() {
        // if this crashes, NeuroID singleton failed to initialize and test will fail
        setNeuroIDInstance()
        NeuroID.getInternalInstance()?.application = null
        setNeuroIDMockedLogger()

        clearLogCounts()
        storedEvents.clear()
        queuedEvents.clear()
        NeuroID.getInternalInstance()?.excludedTestIDList?.clear()
    }

    // NeuroID.init() Tests
    // The init block runs inside the private constructor, exercised via BuilderConfig.build()

    @Test
    fun test_init_serverEnvironment_production_setsProductionEndpoints() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertEquals(Constants.productionEndpoint.displayName, NeuroID.endpoint)
        assertEquals(Constants.productionScriptsEndpoint.displayName, NeuroID.scriptEndpoint)
    }

    @Test
    fun test_init_serverEnvironment_development_setsDevEndpoints() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.DEVELOPMENT)
        ).build()

        assertEquals(Constants.devEndpoint.displayName, NeuroID.endpoint)
        assertEquals(Constants.devScriptsEndpoint.displayName, NeuroID.scriptEndpoint)
    }

    @Test
    fun test_init_serverEnvironment_test_setsTestEndpoints() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.TEST)
        ).build()

        assertEquals(Constants.testScriptEndpoint.displayName, NeuroID.endpoint)
        assertEquals(Constants.testScriptEndpoint.displayName, NeuroID.scriptEndpoint)
    }

    @Test
    fun test_init_serverEnvironment_prodScriptDevCollection_setsMixedEndpoints() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODSCRIPT_DEVCOLLECTION)
        ).build()

        assertEquals(Constants.devEndpoint.displayName, NeuroID.endpoint)
        assertEquals(Constants.productionScriptsEndpoint.displayName, NeuroID.scriptEndpoint)
    }

    @Test
    fun test_init_invalidClientKey_clearsKeyAndSetsInvalidTabID() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("invalid_key", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val instance = NeuroID.getInternalInstance()
        assertEquals("", instance?.clientKey)
        assert(instance?.tabID?.endsWith("-invalid-client-key") == true)
    }

    @Test
    fun test_init_liveClientKey_setsEnvironmentLive() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_live_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertEquals("LIVE", NeuroID.environment)
    }

    @Test
    fun test_init_testClientKey_setsEnvironmentTest() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertEquals("TEST", NeuroID.environment)
    }

    @Test
    fun test_init_nullApplication_doesNotInitialiseApplicationDependentServices() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val instance = NeuroID.getInternalInstance()
        // metaData and nidCallActivityListener are only set inside application?.let block
        assertEquals(null, instance?.metaData)
        assertEquals(null, instance?.application)
    }

    @Test
    fun test_init_validKey_tabIDDoesNotContainInvalidSuffix() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val tabID = NeuroID.getInternalInstance()?.tabID
        assert(tabID?.contains("-invalid-client-key") == false)
        assert(tabID?.isNotEmpty() == true)
    }

    @Test
    fun test_init_coreServicesAlwaysInitialised() {
        // configService, dataStore, identifierService are always initialised regardless of application
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        NeuroID.BuilderConfig(
            null,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val instance = NeuroID.getInternalInstance()
        assertNotNull(instance?.configService)
        assertNotNull(instance?.dataStore)
        assertNotNull(instance?.identifierService)
        assertNotNull(instance?.registrationIdentificationHelper)
        assertNotNull(instance?.nidActivityCallbacks)
        assertNotNull(instance?.nidComposeTextWatcher)
    }

    // Helper used by application?.let init block tests
    private fun buildMockedApplication(): Application {
        val mockedSharedPreferencesEditor = mockk<SharedPreferences.Editor>()
        every { mockedSharedPreferencesEditor.apply() } just runs
        every { mockedSharedPreferencesEditor.putString(any(), any()) } returns mockedSharedPreferencesEditor

        val mockedSharedPreferences = mockk<SharedPreferences>()
        every { mockedSharedPreferences.edit() } returns mockedSharedPreferencesEditor
        every { mockedSharedPreferences.getString(any(), any()) } returns ""

        // Use a relaxed context mock so unstubbed calls (e.g. getPackageManager in NIDMetaData) don't throw
        val mockedContext = mockk<Context>(relaxed = true)
        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences

        val mockedConnectivityManager = mockk<ConnectivityManager>()
        every { mockedConnectivityManager.activeNetworkInfo } returns null
        every { mockedConnectivityManager.activeNetwork } returns null
        every { mockedConnectivityManager.getNetworkCapabilities(any()) } returns null

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedContext.getSystemService(Context.LOCATION_SERVICE) } returns mockk<LocationManager>()

        val mockedApplication = mockk<Application>()
        every { mockedApplication.applicationContext } returns mockedContext
        every { mockedApplication.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedApplication.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedApplication.getSystemService(Context.LOCATION_SERVICE) } returns mockk<LocationManager>()
        every { mockedApplication.registerReceiver(any(), any<IntentFilter>()) } returns null
        every { mockedApplication.registerActivityLifecycleCallbacks(any()) } just runs

        mockkStatic(::getSendingService)
        every { getSendingService(any(), any()) } returns mockk(relaxed = true)

        mockkConstructor(NIDJobServiceManager::class)
        every { anyConstructed<NIDJobServiceManager>().startJob(any(), any()) } just runs
        every { anyConstructed<NIDJobServiceManager>().isStopped() } returns false
        every { anyConstructed<NIDJobServiceManager>().sendEvents(any()) } just runs

        mockkConstructor(NIDSharedPrefsDefaults::class)
        every { anyConstructed<NIDSharedPrefsDefaults>().getClientID() } returns "test-client-id"
        every { anyConstructed<NIDSharedPrefsDefaults>().resetClientID() } returns "test-client-id"
        every { anyConstructed<NIDSharedPrefsDefaults>().getPlatform() } returns "android"
        every { anyConstructed<NIDSharedPrefsDefaults>().getSessionID() } returns ""

        mockkConstructor(NIDSessionService::class)
        every { anyConstructed<NIDSessionService>().resumeCollection() } just runs

        mockkConstructor(LocationService::class)
        mockkConstructor(NIDCallActivityListener::class)

        // RootHelper is called inside NIDMetaData init; mock it to avoid Build.FINGERPRINT NPE on JVM
        mockkConstructor(com.neuroid.tracker.utils.RootHelper::class)
        every { anyConstructed<com.neuroid.tracker.utils.RootHelper>().isProbablyEmulator() } returns false
        every { anyConstructed<com.neuroid.tracker.utils.RootHelper>().isRooted(any()) } returns false

        // getAppMetaData is a top-level function called in captureApplicationMetaData; mock to avoid packageName NPE
        mockkStatic(::getAppMetaData)
        every { getAppMetaData(any(), any(), any()) } returns null

        return mockedApplication
    }

    @Test
    fun test_init_withApplication_initialisesNidJobServiceManager() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val instance = NeuroID.getInternalInstance()
        assertNotNull(instance)
        assertNotNull(instance?.nidJobServiceManager)
    }

    @Test
    fun test_init_withApplication_initialisesSessionService() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertNotNull(NeuroID.getInternalInstance()?.sessionService)
    }

    @Test
    fun test_init_withApplication_initialisesMetaData() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertNotNull(NeuroID.getInternalInstance()?.metaData)
    }

    @Test
    fun test_init_withApplication_initialisesSharedPrefsDefaults() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertNotNull(NeuroID.getInternalInstance()?.sharedPrefsDefaults)
    }

    @Test
    fun test_init_withApplication_initialisesLocationService() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertNotNull(NeuroID.getInternalInstance()?.locationService)
    }

    @Test
    fun test_init_withApplication_initialisesNidCallActivityListener() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertNotNull(NeuroID.getInternalInstance()?.nidCallActivityListener)
    }

    @Test
    fun test_init_withApplication_setsNetworkConnectionType() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        val networkType = NeuroID.getInternalInstance()?.networkConnectionType
        assertNotNull(networkType)
        assert(networkType!!.isNotEmpty())
    }

    @Test
    fun test_init_withApplication_isConnected_whenNetworkInfoNull() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertEquals(false, NeuroID.getInternalInstance()?.isConnected)
    }

    @Test
    fun test_init_withApplication_isConnected_whenNetworkInfoConnected() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        val mockedConnectivityManager = mockedApplication.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mockedNetworkInfo = mockk<NetworkInfo>(relaxed = true)
        every { mockedNetworkInfo.isConnectedOrConnecting } returns true
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        assertEquals(true, NeuroID.getInternalInstance()?.isConnected)
    }

    @Test
    fun test_init_withApplication_isNotAdvancedDevice_doesNotCallResetClientId() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", false, "", true, NeuroID.PRODUCTION)
        ).build()

        verify(exactly = 0) {
            anyConstructed<NIDSharedPrefsDefaults>().resetClientID()
        }
    }

    @Test
    fun test_init_withApplication_isAdvancedDevice_callsResetClientId() {
        NeuroID._isSDKStarted = false
        NeuroID.setSingletonNull()
        val mockedApplication = buildMockedApplication()

        NeuroID.BuilderConfig(
            mockedApplication,
            NIDConfiguration("key_test_fake1234", true, "", true, NeuroID.PRODUCTION)
        ).build()

        verify(atLeast = 1) {
            anyConstructed<NIDSharedPrefsDefaults>().resetClientID()
        }

        // Explicitly unmock constructors before tearDown's unmockkAll() to avoid
        // ConcurrentModificationException in MockK 1.12.0 when coroutines are involved
        unmockkConstructor(
            NIDJobServiceManager::class,
            NIDSharedPrefsDefaults::class,
            NIDSessionService::class,
            LocationService::class,
            NIDCallActivityListener::class,
            com.neuroid.tracker.utils.RootHelper::class,
        )
        unmockkStatic(::getSendingService, ::getAppMetaData)
    }

    // Class Init Test
    @Test
    fun test_configure_tab_id() {
        setMockedNIDJobServiceManager(false)
        val ogTabID = NeuroID.getInternalInstance()?.tabID

        NeuroID.getInternalInstance()?.resetSingletonInstance()

        assert(ogTabID != NeuroID.getInternalInstance()?.tabID)
    }

    // setNeuroIDInstance Tests
    @Test
    fun test_setNeuroIDInstance_firstTime() {
        // Set singleton to null to simulate first-time initialization
        NeuroID.setSingletonNull()

        // Create a mocked NeuroID instance
        val mockedNeuroID = mockk<NeuroID>(relaxed = true)
        val mockedLogger = mockk<NIDLogWrapper>()

        // Mock the necessary properties and methods
        every { mockedNeuroID.isAdvancedDevice } returns false
        every { mockedNeuroID.logger } returns mockedLogger
        every { mockedNeuroID.setupCallbacks() } just runs
        every {
            mockedNeuroID.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just runs
        every { mockedLogger.e(any(), any()) } just runs

        // Call setNeuroIDInstance
        NeuroID.setNeuroIDInstance(mockedNeuroID)

        // Verify setupCallbacks was called
        verify(exactly = 1) {
            mockedNeuroID.setupCallbacks()
        }

        // Verify logger.e was NOT called (no error)
        verify(exactly = 0) {
            mockedLogger.e(any(), any())
        }

        // Verify captureEvent was NOT called with error
        verify(exactly = 0) {
            mockedNeuroID.captureEvent(
                queuedEvent = any(),
                type = LOG,
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = any(),
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = any(),
                hv = any(),
                en = any(),
                etn = any(),
                ec = any(),
                et = any(),
                eid = any(),
                ct = any(),
                sm = any(),
                pd = any(),
                x = any(),
                y = any(),
                w = any(),
                h = any(),
                sw = any(),
                sh = any(),
                f = any(),
                lsid = any(),
                sid = any(),
                siteId = any(),
                cid = any(),
                did = any(),
                iid = any(),
                loc = any(),
                ua = any(),
                tzo = any(),
                lng = any(),
                ce = any(),
                je = any(),
                ol = any(),
                p = any(),
                dnt = any(),
                tch = any(),
                url = any(),
                ns = any(),
                jsl = any(),
                jsv = any(),
                uid = any(),
                o = any(),
                rts = any(),
                metadata = any(),
                rid = any(),
                m = "NeuroID SDK should only be built once.",
                level = "ERROR",
                c = any(),
                isWifi = any(),
                isConnected = any(),
                cp = any(),
                l = any(),
                synthetic = any(),
            )
        }

        // Verify getInstance returns the set instance
        assertEquals(mockedNeuroID, NeuroID.getInstance())
    }

    @Test
    fun test_setNeuroIDInstance_duplicate() {
        // Initialize with a first instance
        NeuroID.setSingletonNull()
        val firstNeuroID = mockk<NeuroID>(relaxed = true)
        val firstLogger = mockk<NIDLogWrapper>()

        every { firstNeuroID.isAdvancedDevice } returns false
        every { firstNeuroID.logger } returns firstLogger
        every { firstNeuroID.setupCallbacks() } just runs
        every {
            firstNeuroID.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just runs
        every { firstLogger.e(any(), any()) } just runs

        NeuroID.setNeuroIDInstance(firstNeuroID)

        // Now try to set a second instance
        val secondNeuroID = mockk<NeuroID>(relaxed = true)
        val secondLogger = mockk<NIDLogWrapper>()

        every { secondNeuroID.isAdvancedDevice } returns false
        every { secondNeuroID.logger } returns secondLogger
        every { secondNeuroID.setupCallbacks() } just runs
        every { secondLogger.e(any(), any()) } just runs

        NeuroID.setNeuroIDInstance(secondNeuroID)

        // Verify logger.e was called with error message
        verify(exactly = 1) {
            firstLogger.e("NeuroID", "NeuroID SDK should only be built once.")
        }

        // Verify captureEvent was called with error
        verify(exactly = 1) {
            firstNeuroID.captureEvent(
                queuedEvent = any(),
                type = LOG,
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = any(),
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = any(),
                hv = any(),
                en = any(),
                etn = any(),
                ec = any(),
                et = any(),
                eid = any(),
                ct = any(),
                sm = any(),
                pd = any(),
                x = any(),
                y = any(),
                w = any(),
                h = any(),
                sw = any(),
                sh = any(),
                f = any(),
                lsid = any(),
                sid = any(),
                siteId = any(),
                cid = any(),
                did = any(),
                iid = any(),
                loc = any(),
                ua = any(),
                tzo = any(),
                lng = any(),
                ce = any(),
                je = any(),
                ol = any(),
                p = any(),
                dnt = any(),
                tch = any(),
                url = any(),
                ns = any(),
                jsl = any(),
                jsv = any(),
                uid = any(),
                o = any(),
                rts = any(),
                metadata = any(),
                rid = any(),
                m = "NeuroID SDK should only be built once.",
                level = "ERROR",
                c = any(),
                isWifi = any(),
                isConnected = any(),
                cp = any(),
                l = any(),
                synthetic = any(),
            )
        }

        // Verify the singleton is still the first instance (not replaced)
        assertEquals(firstNeuroID, NeuroID.getInstance())

        // Verify setupCallbacks was NOT called on the second instance
        verify(exactly = 0) {
            secondNeuroID.setupCallbacks()
        }
    }

    @Test
    fun test_setNeuroIDInstance_withAdvancedDevice() {
        // Set singleton to null to simulate first-time initialization
        NeuroID.setSingletonNull()

        // Create a mocked NeuroID instance with advanced device enabled
        val mockedNeuroID = mockk<NeuroID>(relaxed = true)
        val mockedLogger = mockk<NIDLogWrapper>()

        every { mockedNeuroID.isAdvancedDevice } returns true
        every { mockedNeuroID.logger } returns mockedLogger
        every { mockedNeuroID.setupCallbacks() } just runs
        every { mockedNeuroID.checkThenCaptureAdvancedDevice(any()) } just runs
        every {
            mockedNeuroID.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just runs
        every { mockedLogger.e(any(), any()) } just runs

        // Call setNeuroIDInstance
        NeuroID.setNeuroIDInstance(mockedNeuroID)

        // Verify checkThenCaptureAdvancedDevice was called with true
        verify(exactly = 1) {
            mockedNeuroID.checkThenCaptureAdvancedDevice(true)
        }

        // Verify setupCallbacks was called
        verify(exactly = 1) {
            mockedNeuroID.setupCallbacks()
        }

        // Verify getInstance returns the set instance
        assertEquals(mockedNeuroID, NeuroID.getInstance())
    }

    private fun setupAttemptedLoginTestEnvironment(validID: Boolean = false) {
        // fake out the clock
        mockkStatic(Calendar::class)
        every { Calendar.getInstance().timeInMillis } returns 1
        // make the logger not throw
        val logger = mockk<NIDLogWrapper>()
        every { logger.e(any(), any()) } just runs
        NeuroID.getInternalInstance()?.logger = logger

        // make the validation service throw
        val mockIdentificationService = getMockedIdentifierService()
        every {
            mockIdentificationService.setGenericUserID(
                any(),
                any(),
                any(),
                any()
            )
        } returns validID
        NeuroID.getInternalInstance()?.identifierService = mockIdentificationService
        setMockedDataStore()
        setMockedNIDJobServiceManager(false)
        NeuroID._isSDKStarted = true
    }

    private fun testAttemptedLogin(
        userId: String?,
        expectedUserId: String,
        expectedFailedResult: Boolean,
    ) {
        setupAttemptedLoginTestEnvironment(!expectedFailedResult)
        val dataStoreManager = NeuroID.getInternalInstance()?.dataStore
        val mockIdentificationService = NeuroID.getInternalInstance()?.identifierService

        val actualResult = NeuroID.getInstance()?.attemptedLogin(userId)
        verify {
            mockIdentificationService?.setGenericUserID(
                any(),
                "ATTEMPTED_LOGIN",
                userId ?: "scrubbed-id-failed-validation",
                userId != null
            )
        }

        if (expectedFailedResult) {
            dataStoreManager?.saveEvent(
                NIDEventModel(
                    ts = 1,
                    type = "ATTEMPTED_LOGIN",
                    uid = expectedUserId
                )
            )
        }

        assert(actualResult == true)
        unmockkStatic(Calendar::class)
    }

    @Test
    fun testAttemptedLoginVarious() {
        // the single good id
        testAttemptedLogin("goodone", "goodone", false)
        // all the rest are rubbish ids
        testAttemptedLogin("12", "scrubbed-id-failed-validation", true)
        testAttemptedLogin("test@test.com'", "scrubbed-id-failed-validation", true)
        testAttemptedLogin(null, "scrubbed-id-failed-validation", true)
        testAttemptedLogin("@#\$%^&*()", "scrubbed-id-failed-validation", true)
        testAttemptedLogin(
            "¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂ",
            "scrubbed-id-failed-validation",
            true
        )
        testAttemptedLogin(
            "ÃÄÅÆÇÈÉ ÊË Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö",
            "scrubbed-id-failed-validation",
            true
        )
        testAttemptedLogin("almost good", "scrubbed-id-failed-validation", true)
    }

    // start() Tests
    @Test
    fun test_start_success() {
        // Setup mocks
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService
        NeuroID._isSDKStarted = false

        // Mock start to call completion with true
        every { mockedSessionService.start(siteID = null, completion = any()) } answers {
            val completion = secondArg<(Boolean) -> Unit>()
            completion(true)
        }

        var completionResult: Boolean? = null
        NeuroID.getInstance()?.start { result ->
            completionResult = result
        }

        // Verify sessionService.start was called
        verify(exactly = 1) {
            mockedSessionService.start(siteID = null, completion = any())
        }

        // Verify completion callback was invoked with true
        assertEquals(true, completionResult)
    }

    @Test
    fun test_start_alreadyStarted() {
        // Setup mocks
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService
        NeuroID._isSDKStarted = true

        // Mock start to call completion with false (already started)
        every { mockedSessionService.start(siteID = null, completion = any()) } answers {
            val completion = secondArg<(Boolean) -> Unit>()
            completion(false)
        }

        var completionResult: Boolean? = null
        NeuroID.getInstance()?.start { result ->
            completionResult = result
        }

        // Verify sessionService.start was called
        verify(exactly = 1) {
            mockedSessionService.start(siteID = null, completion = any())
        }

        // Verify completion callback was invoked with false
        assertEquals(false, completionResult)
    }

    // stop() Tests
    @Test
    fun test_stop_success() {
        // Setup mocks
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService
        NeuroID._isSDKStarted = true

        // Mock stop to return true
        every { mockedSessionService.stop() } returns true

        val result = NeuroID.getInstance()?.stop()

        // Verify sessionService.stop was called
        verify(exactly = 1) {
            mockedSessionService.stop()
        }

        // Verify result is true
        assertEquals(true, result)
    }

    @Test
    fun test_stop_whenNotStarted() {
        // Setup mocks
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService
        NeuroID._isSDKStarted = false

        // Mock stop to return true
        every { mockedSessionService.stop() } returns true

        val result = NeuroID.getInstance()?.stop()

        // Verify sessionService.stop was called
        verify(exactly = 1) {
            mockedSessionService.stop()
        }

        // Verify result is true
        assertEquals(true, result)
    }

    // resetClientId() Tests
    @Test
    fun test_resetClientId_withApplication() {
        // Setup mocked application and SharedPreferences
        val mockedApplication = getMockedApplication()
        NeuroID.getInternalInstance()?.application = mockedApplication

        // Mock NIDSharedPrefsDefaults
        mockkConstructor(NIDSharedPrefsDefaults::class)
        val newClientID = "new-client-id-12345"
        every { anyConstructed<NIDSharedPrefsDefaults>().resetClientID() } returns newClientID

        // Get original clientID
        val originalClientID = NeuroID.getInternalInstance()?.clientID

        // Call resetClientId
        val mockNIDSharedPrefsDefaults = mockk<NIDSharedPrefsDefaults>()
        every{mockNIDSharedPrefsDefaults.resetClientID()} returns "new-client-id-12345"
        NeuroID.getInternalInstance()?.sharedPrefsDefaults = mockNIDSharedPrefsDefaults
        NeuroID.getInternalInstance()?.resetClientId()

        // Verify clientID was updated
        assertEquals(newClientID, NeuroID.getInternalInstance()?.clientID)
        assert(originalClientID != NeuroID.getInternalInstance()?.clientID)

        // Verify resetClientID was called
        verify(exactly = 1) {
            mockNIDSharedPrefsDefaults.resetClientID()
        }

        unmockkConstructor(NIDSharedPrefsDefaults::class)
    }

    @Test
    fun test_resetClientId_withoutApplication() {
        // Ensure application is null
        NeuroID.getInternalInstance()?.application = null

        // Get original clientID
        val originalClientID = NeuroID.getInternalInstance()?.clientID

        // Call resetClientId (should do nothing since application is null)
        NeuroID.getInternalInstance()?.resetClientId()

        // Verify clientID was NOT changed
        assertEquals(originalClientID, NeuroID.getInternalInstance()?.clientID)
    }

    // getUserID() Tests
    @Test
    fun test_getUserID_returnsUserID() {
        val testUserID = "test-user-456"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock getUserID to return test user ID
        every { mockedIdentifierService.getUserID(any()) } returns testUserID

        val result = NeuroID.getInstance()?.getUserID()

        // Verify identifierService.getUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.getUserID(any())
        }

        // Verify result matches
        assertEquals(testUserID, result)
    }

    @Test
    fun test_getUserID_returnsEmptyString() {
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock getUserID to return empty string
        every { mockedIdentifierService.getUserID(any()) } returns ""

        val result = NeuroID.getInstance()?.getUserID()

        // Verify result is empty
        assertEquals("", result)
    }

    // setUserID() Tests
    @Test
    fun test_identify_success() {
        val testUserID = "valid-user-id-789"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return true
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns true

        val result = NeuroID.getInstance()?.identify(testUserID)

        // Verify identifierService.setUserID was called with correct parameters
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), testUserID, true)
        }

        // Verify result is true
        assertEquals(true, result)
    }

    @Test
    fun test_identify_failure() {
        val invalidUserID = "invalid id"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return false (validation failed)
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns false

        val result = NeuroID.getInstance()?.identify(invalidUserID)

        // Verify identifierService.setUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), invalidUserID, true)
        }

        // Verify result is false
        assertEquals(false, result)
    }

    @Test
    fun test_identify_emptyString() {
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return false for empty string
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns false

        val result = NeuroID.getInstance()?.identify("")

        // Verify identifierService.setUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), "", true)
        }

        // Verify result is false
        assertEquals(false, result)
    }

    // setUserID() Tests
    @Test
    fun test_setUserID_success() {
        val testUserID = "valid-user-id-789"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return true
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns true

        val result = NeuroID.getInstance()?.setUserID(testUserID)

        // Verify identifierService.setUserID was called with correct parameters
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), testUserID, true)
        }

        // Verify result is true
        assertEquals(true, result)
    }

    @Test
    fun test_setUserID_failure() {
        val invalidUserID = "invalid id"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return false (validation failed)
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns false

        val result = NeuroID.getInstance()?.setUserID(invalidUserID)

        // Verify identifierService.setUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), invalidUserID, true)
        }

        // Verify result is false
        assertEquals(false, result)
    }

    @Test
    fun test_setUserID_emptyString() {
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setUserID to return false for empty string
        every { mockedIdentifierService.setUserID(any(), any(), any()) } returns false

        val result = NeuroID.getInstance()?.setUserID("")

        // Verify identifierService.setUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setUserID(any(), "", true)
        }

        // Verify result is false
        assertEquals(false, result)
    }

    // getRegisteredUserID() Tests
    @Test
    fun test_getRegisteredUserID_returnsRegisteredUserID() {
        val testRegisteredUserID = "registered-user-123"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock getRegisteredUserID to return test registered user ID
        every { mockedIdentifierService.getRegisteredUserID(any()) } returns testRegisteredUserID

        val result = NeuroID.getInstance()?.getRegisteredUserID()

        // Verify identifierService.getRegisteredUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.getRegisteredUserID(any())
        }

        // Verify result matches
        assertEquals(testRegisteredUserID, result)
    }

    @Test
    fun test_getRegisteredUserID_returnsEmptyString() {
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock getRegisteredUserID to return empty string
        every { mockedIdentifierService.getRegisteredUserID(any()) } returns ""

        val result = NeuroID.getInstance()?.getRegisteredUserID()

        // Verify result is empty
        assertEquals("", result)
    }

    // setRegisteredUserID() Tests
    @Test
    fun test_setRegisteredUserID_success() {
        val testRegisteredUserID = "valid-registered-user-456"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setRegisteredUserID to return true
        every { mockedIdentifierService.setRegisteredUserID(any(), any()) } returns true

        val result = NeuroID.getInstance()?.setRegisteredUserID(testRegisteredUserID)

        // Verify identifierService.setRegisteredUserID was called with correct parameters
        verify(exactly = 1) {
            mockedIdentifierService.setRegisteredUserID(any(), testRegisteredUserID)
        }

        // Verify result is true
        assertEquals(true, result)
    }

    @Test
    fun test_setRegisteredUserID_failure() {
        val invalidRegisteredUserID = "invalid registered id"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setRegisteredUserID to return false (validation failed)
        every { mockedIdentifierService.setRegisteredUserID(any(), any()) } returns false

        val result = NeuroID.getInstance()?.setRegisteredUserID(invalidRegisteredUserID)

        // Verify identifierService.setRegisteredUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setRegisteredUserID(any(), invalidRegisteredUserID)
        }

        // Verify result is false
        assertEquals(false, result)
    }

    @Test
    fun test_setRegisteredUserID_emptyString() {
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setRegisteredUserID to return false for empty string
        every { mockedIdentifierService.setRegisteredUserID(any(), any()) } returns false

        val result = NeuroID.getInstance()?.setRegisteredUserID("")

        // Verify identifierService.setRegisteredUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.setRegisteredUserID(any(), "")
        }

        // Verify result is false
        assertEquals(false, result)
    }

    @Test
    fun test_setRegisteredUserID_multipleCalls() {
        val firstUserID = "first-user"
        val secondUserID = "second-user"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock setRegisteredUserID to return true both times
        every { mockedIdentifierService.setRegisteredUserID(any(), any()) } returns true

        // First call
        val firstResult = NeuroID.getInstance()?.setRegisteredUserID(firstUserID)

        // Second call (should log warning about multiple registered user IDs)
        val secondResult = NeuroID.getInstance()?.setRegisteredUserID(secondUserID)

        // Verify identifierService.setRegisteredUserID was called twice
        verify(exactly = 2) {
            mockedIdentifierService.setRegisteredUserID(any(), any())
        }

        // Verify both calls succeeded
        assertEquals(true, firstResult)
        assertEquals(true, secondResult)
    }

    @Test
    fun testSetTestURL() {
        val testUrl = "https://test.example.com"

        // Setup mocked application with SharedPreferences
        val mockedApplication = getMockedApplication()
        NeuroID.getInternalInstance()?.application = mockedApplication

        // Setup mocked job service manager with setTestEventSender
        val mockedJobServiceManager = mockk<NIDJobServiceManager>()
        every { mockedJobServiceManager.setTestEventSender(any()) } just runs
        every { mockedJobServiceManager.startJob(any(), any()) } just runs
        every { mockedJobServiceManager.isStopped() } returns true
        every { mockedJobServiceManager.stopJob() } just runs
        coEvery { mockedJobServiceManager.sendEvents(any()) } just runs

        NeuroID.getInternalInstance()?.setNIDJobServiceManager(mockedJobServiceManager)

        // Call setTestURL
        NeuroID.getInstance()?.setTestURL(testUrl)

        // Verify endpoint was set
        assertEquals(testUrl, NeuroID.endpoint)
        assertEquals(Constants.devScriptsEndpoint.displayName, NeuroID.scriptEndpoint)

        // Verify setTestEventSender was called on the job service manager
        verify(exactly = 1) {
            mockedJobServiceManager.setTestEventSender(any())
        }
    }

    @Test
    fun testSetTestURL_withoutApplication() {
        val testUrl = "https://test.example.com"

        // Setup mocked job service manager
        val mockedJobServiceManager = mockk<NIDJobServiceManager>()
        every { mockedJobServiceManager.setTestEventSender(any()) } just runs
        every { mockedJobServiceManager.startJob(any(), any()) } just runs
        every { mockedJobServiceManager.isStopped() } returns true
        every { mockedJobServiceManager.stopJob() } just runs
        coEvery { mockedJobServiceManager.sendEvents(any()) } just runs

        NeuroID.getInternalInstance()?.setNIDJobServiceManager(mockedJobServiceManager)

        // Ensure application is null
        NeuroID.getInternalInstance()?.application = null

        // Call setTestURL
        NeuroID.getInstance()?.setTestURL(testUrl)

        // Verify endpoint was set
        assertEquals(testUrl, NeuroID.endpoint)
        assertEquals(Constants.devScriptsEndpoint.displayName, NeuroID.scriptEndpoint)

        // Verify setTestEventSender was NOT called since application is null
        verify(exactly = 0) {
            mockedJobServiceManager.setTestEventSender(any())
        }
    }

    //   setTestingNeuroIDDevURL
    @Test
    fun testSetTestingNeuroIDDevURL() {
        NeuroID.getInstance()?.setTestingNeuroIDDevURL()

        assertEquals(true, NeuroID.endpoint == Constants.devEndpoint.displayName)
    }

    //    setScreenName
    @Test
    fun testSetScreenName_success() {
        NeuroID._isSDKStarted = true
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        val value = NeuroID.getInstance()?.setScreenName("testName")

        assertEquals(true, value)
        verify(exactly = 1) {
            mockedSessionService.createMobileMetadata()
        }
    }

    @Test
    fun testSetScreenName_failure() {
        setNeuroIDMockedLogger(errorMessage = "NeuroID SDK is not started")
        NeuroID._isSDKStarted = false

        val value = NeuroID.getInstance()?.setScreenName("testName")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    getScreenName
    @Test
    fun testGetScreenName() {
        val expectedValue = "myScreen"
        NeuroID.screenName = expectedValue

        val value = NeuroID.getInstance()?.getScreenName()

        assertEquals(expectedValue, value)
    }

    //    excludeViewByTestID
    @Test
    fun testExcludeViewByTestID_single() {
        NeuroID.getInstance()?.excludeViewByTestID("excludeMe")

        assertEquals(1, NeuroID.getInternalInstance()?.excludedTestIDList?.count())
        assertEquals("excludeMe", NeuroID.getInternalInstance()?.excludedTestIDList?.first())
    }

    @Test
    fun testExcludeViewByTestID_double() {
        NeuroID.getInstance()?.excludeViewByTestID("excludeMe")
        assertEquals(1, NeuroID.getInternalInstance()?.excludedTestIDList?.count())
        assertEquals("excludeMe", NeuroID.getInternalInstance()?.excludedTestIDList?.first())

        NeuroID.getInstance()?.excludeViewByTestID("excludeMe")
        assertEquals(1, NeuroID.getInternalInstance()?.excludedTestIDList?.count())
    }

    //    getEnvironment
    @Test
    fun testGetEnvironment() {
        val expectedValue = "MyEnv"
        NeuroID.environment = expectedValue

        val value = NeuroID.getInstance()?.getEnvironment()

        assertEquals(expectedValue, value)
    }

    //    getSessionID
    @Test
    fun testGetSessionID() {
        val expectedValue = "testSessionID"
        NeuroID.getInternalInstance()?.userID = expectedValue

        val value = NeuroID.getInternalInstance()?.getSessionID()

        assertEquals(expectedValue, value)
    }

    @Test
    fun test_getSessionID_returnsEmptyString() {
        NeuroID.getInternalInstance()?.userID = ""

        val value = NeuroID.getInternalInstance()?.getSessionID()

        assertEquals("", value)
    }

    @Test
    fun test_getSessionID_returnsUserID_notSessionID() {
        // Explicitly verify that getSessionID() returns userID and NOT sessionID
        // This covers the behavioral change where getSessionID() was updated to return userID
        val testUserID = "user-id-value"
        val testSessionID = "session-id-value"
        NeuroID.getInternalInstance()?.userID = testUserID
        NeuroID.getInternalInstance()?.sessionID = testSessionID

        val value = NeuroID.getInternalInstance()?.getSessionID()

        assertEquals(testUserID, value)
        assertNotEquals(testSessionID, value)
    }

    //    getClientID
    @Test
    fun testGetClientID() {
        val expectedValue = "testClientID"
        NeuroID.getInternalInstance()?.clientID = expectedValue

        val value = NeuroID.getInstance()?.getClientID()

        assertEquals(expectedValue, value)
    }

    //    shouldForceStart
    @Test
    fun test_shouldForceStart_false() {
        val expectedValue = false

        val value = NeuroID.getInternalInstance()?.shouldForceStart()

        assertEquals(expectedValue, value)
    }

    @Test
    fun test_shouldForceStart_true() {
        val expectedValue = true
        NeuroID.getInternalInstance()?.forceStart = expectedValue

        val value = NeuroID.getInternalInstance()?.shouldForceStart()

        assertEquals(expectedValue, value)
    }

    //    registerPageTargets
    @Test
    fun testRegisterPageTargets() {
        val mockedNIDACB = mockk<ActivityCallbacks>()
        every { mockedNIDACB.forceStart(any()) } just runs
        NeuroID.getInternalInstance()?.setNIDActivityCallbackInstance(mockedNIDACB)

        val mockedActivity = mockk<Activity>()

        NeuroID.getInstance()?.registerPageTargets(mockedActivity)

        assertEquals(true, NeuroID.getInternalInstance()?.forceStart)
        verify { mockedNIDACB.forceStart(mockedActivity) }

        // reset for other tests
        NeuroID.getInternalInstance()?.forceStart = false
    }

    //    getTabId
    @Test
    fun testGetTabId() {
        val expectedValue = "MyRNDID"

        NeuroID.getInternalInstance()?.tabID = expectedValue

        val value = NeuroID.getInternalInstance()?.getTabId()

        assertEquals(expectedValue, value)
    }

    //    getFirstTS - not worth testing
    @Test
    fun testGetFirstTS() {
        val expectedValue: Long = 1234

        NeuroID.getInternalInstance()?.timestamp = expectedValue

        val value = NeuroID.getInternalInstance()?.getFirstTS()

        assertEquals(expectedValue, value)
    }

//    closeSession - Need to mock NIDJobServiceManager
//    resetClientId - Need to mock Application & Shared Preferences

    //    isStopped
    @Test
    fun testIsStopped_true() {
        setMockedNIDJobServiceManager()

        val expectedValue = true
        NeuroID._isSDKStarted = !expectedValue

        val value = NeuroID.getInstance()?.isStopped()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testIsStopped_false() {
        setMockedNIDJobServiceManager()

        val expectedValue = false
        NeuroID._isSDKStarted = !expectedValue

        val value = NeuroID.getInstance()?.isStopped()

        assertEquals(expectedValue, value)
    }

    //    registerTarget - Need to mock Activity

    //    getApplicationContext - Need to mock Application
    @Test
    fun testGetApplicationContext() {
        val mockedApplication = mockk<Application>()
        val mockedContext = mockk<Context>()
        every { mockedApplication.applicationContext } answers {
            mockedContext
        }
        NeuroID.getInternalInstance()?.application = mockedApplication

        var value = NeuroID.getInternalInstance()?.getApplicationContext()
        assertEquals(mockedContext, value)
    }

    //    getNetworkType
    @Test
    fun testGetNetworkType_wifi_23() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_WIFI

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns true
        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager

        val networkType = neuroID?.getNetworkType(mockedContext, 23)

        assertEquals("wifi", networkType)
    }

    @Test
    fun testGetNetworkType_cellular_23() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_MOBILE

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager

        val networkType = neuroID?.getNetworkType(mockedContext, 23)

        assertEquals("cell", networkType)
    }

    @Test
    fun testGetNetworkType_ethernet_23() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_ETHERNET

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) } returns true

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 23)

        assertEquals("eth", networkType)
    }

    @Test
    fun testGetNetworkType_unknown_23() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns 645645

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 23)

        assertEquals("unknown", networkType)
    }

    @Test
    fun testGetNetworkType_noNetwork_23() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()

        every { mockedConnectivityManager.activeNetworkInfo } returns null
        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns null

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 23)

        assertEquals("noNetwork", networkType)
    }

    @Test
    fun testGetNetworkType_wifi_22() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_WIFI

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns true
        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager

        val networkType = neuroID?.getNetworkType(mockedContext, 22)

        assertEquals("wifi", networkType)
    }

    @Test
    fun testGetNetworkType_cellular_22() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_MOBILE

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager

        val networkType = neuroID?.getNetworkType(mockedContext, 22)

        assertEquals("cell", networkType)
    }

    @Test
    fun testGetNetworkType_ethernet_22() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns android.net.ConnectivityManager.TYPE_ETHERNET

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) } returns true

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 22)

        assertEquals("eth", networkType)
    }

    @Test
    fun testGetNetworkType_unknown_22() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()
        val mockedNetwork = mockk<android.net.Network>()
        val mockedNetworkCapabilities = mockk<android.net.NetworkCapabilities>()
        val mockedNetworkInfo = mockk<NetworkInfo>()
        every { mockedNetworkInfo.type } returns 645645

        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns mockedNetwork
        every { mockedConnectivityManager.activeNetworkInfo } returns mockedNetworkInfo
        every { mockedConnectivityManager.getNetworkCapabilities(mockedNetwork) } returns mockedNetworkCapabilities
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockedNetworkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 22)

        assertEquals("unknown", networkType)
    }

    @Test
    fun testGetNetworkType_noNetwork_22() {
        val mockedContext = mockk<Context>()
        val mockedConnectivityManager = mockk<android.net.ConnectivityManager>()

        every { mockedConnectivityManager.activeNetworkInfo } returns null
        every { mockedContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockedConnectivityManager
        every { mockedConnectivityManager.activeNetwork } returns null

        val neuroID = NeuroID.getInternalInstance()
        neuroID?.connectivityManager = mockedConnectivityManager
        val networkType = neuroID?.getNetworkType(mockedContext, 22)

        assertEquals("noNetwork", networkType)
    }

//    createSession - Need to mock Application
//    createMobileMetadata - Need to mock Application

    // setIsRN
    @Test
    fun testSetIsRN() {
        NeuroID.getInternalInstance()?.isRN = false
        assertEquals(false, NeuroID.getInternalInstance()?.isRN)

        NeuroID.getInternalInstance()?.setIsRN("0.74.5")

        assertEquals(true, NeuroID.getInternalInstance()?.isRN)
        assertEquals("0.74.5", NeuroID.getInternalInstance()?.rnVersion)
    }

    //    enableLogging
    @Test
    fun testEnableLogging_true() {
        NeuroID.showLogs = false

        NeuroID.getInstance()?.enableLogging(true)

        assertEquals(true, NeuroID.showLogs)
    }

    @Test
    fun testEnableLogging_false() {
        NeuroID.showLogs = true

        NeuroID.getInstance()?.enableLogging(false)

        assertEquals(false, NeuroID.showLogs)
    }

    @Test
    fun test_setVariable() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()

        NeuroID.getInstance()?.setVariable("test", "value")

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === SET_VARIABLE)
        assertEquals(true, storedEvents.firstOrNull()?.key === "test")
        assertEquals(true, storedEvents.firstOrNull()?.v === "value")
    }

    //    captureEvent
    @Test
    fun testCaptureEvent_success() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent")

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_success_queued() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(queuedEvent = true, type = "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(1, queuedEvents.count())
        assertEquals(true, queuedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_failure_not_started() {
        NeuroID._isSDKStarted = false
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_jobManager_stopped() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(true)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tgs() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent", tgs = "excludeMe")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tg() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroID.getInternalInstance()?.captureEvent(
            type = "testEvent",
            tg =
                mapOf(
                    "tgs" to "excludeMe",
                ),
        )

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_lowMemory() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroID.getInternalInstance()?.lowMemory = true

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent")

        assertEquals(0, storedEvents.count())

        assertWarningCount(1)

        NeuroID.getInternalInstance()?.lowMemory = false
    }

    @Test
    fun testIncrementPacketNumber() {
        NeuroID.getInternalInstance()?.let {
            val before = it.packetNumber
            it.incrementPacketNumber()
            val after = it.packetNumber
            assert(after - before == 1)
        }
    }

    @Test
    fun testCaptureEvent_failure_fullBuffer() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore(true)
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroID.getInternalInstance()?.captureEvent(type = "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(0, storedEvents.count())

        assertWarningCount(1)
    }

    @Test
    fun test_captureApplicationMetaData() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()

        // Mock the application context
        val mockContext = mockk<Context>(relaxed = true)
        val mockPackageManager = mockk<android.content.pm.PackageManager>(relaxed = true)
        val mockPackageInfo = mockk<android.content.pm.PackageInfo>(relaxed = true)
        val mockApplicationInfo = mockk<android.content.pm.ApplicationInfo>(relaxed = true)

        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.packageName } returns "com.test.app"
        every { mockContext.applicationInfo } returns mockApplicationInfo
        every { mockPackageManager.getPackageInfo(any<String>(), any<Int>()) } returns mockPackageInfo
        mockPackageInfo.versionName = "1.2.3"
        mockPackageInfo.packageName = "com.test.app"
        mockPackageInfo.applicationInfo = mockApplicationInfo
        mockApplicationInfo.name = "TestApp"
        mockApplicationInfo.minSdkVersion = 24
        mockApplicationInfo.targetSdkVersion = 34

        // Set public field AFTER all every blocks are complete to avoid MockK interception
        @Suppress("DEPRECATION")
        mockPackageInfo.versionCode = 123

        val mockApplication = mockk<Application>(relaxed = true)
        every { mockApplication.applicationContext } returns mockContext

        NeuroID.getInternalInstance()?.application = mockApplication
        NeuroID.getInternalInstance()?.rnVersion = "0.72.0"

        // Call captureApplicationMetaData
        val mockNIDSharedPrefsDefaults = mockk<NIDSharedPrefsDefaults>()
        every{mockNIDSharedPrefsDefaults.getPlatform()}  returns "Android"
        NeuroID.getInternalInstance()?.sharedPrefsDefaults = mockNIDSharedPrefsDefaults
        NeuroID.getInternalInstance()?.captureApplicationMetaData()

        // Verify event was captured
        assertEquals(1, storedEvents.count())
        val event = storedEvents.firstOrNull()
        assertEquals(APPLICATION_METADATA, event?.type)

        // Verify the attrs contain the new parameters
        val attrs = event?.attrs
        assert(attrs != null)
        assert(attrs!!.isNotEmpty())

        // Check for rnVersion
        val hostRNVersionAttr = attrs.find { it["n"] == "rnVersion" }
        assertEquals("0.72.0", hostRNVersionAttr?.get("v"))

        // Check for minOSVersion
        val hostMinSDKLevelAttr = attrs.find { it["n"] == "minOSVersion" }
        assertEquals(24, hostMinSDKLevelAttr?.get("v"))

        // Check for original parameters
        val versionNameAttr = attrs.find { it["n"] == "versionName" }
        assertEquals("1.2.3", versionNameAttr?.get("v"))

        val versionNumberAttr = attrs.find { it["n"] == "versionNumber" }
        assertEquals(123, versionNumberAttr?.get("v"))
    }

    @Test
    fun test_captureApplicationMetaData_withDefaults() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()

        // Mock the application context with default RN version
        val mockContext = mockk<Context>(relaxed = true)
        val mockPackageManager = mockk<android.content.pm.PackageManager>(relaxed = true)
        val mockPackageInfo = mockk<android.content.pm.PackageInfo>(relaxed = true)
        val mockApplicationInfo = mockk<android.content.pm.ApplicationInfo>(relaxed = true)

        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.packageName } returns "com.test.app"
        every { mockContext.applicationInfo } returns mockApplicationInfo
        every { mockPackageManager.getPackageInfo(any<String>(), any<Int>()) } returns mockPackageInfo
        mockPackageInfo.versionName = "2.0.0"
        mockPackageInfo.packageName = "com.test.app"
        mockPackageInfo.applicationInfo = mockApplicationInfo
        mockApplicationInfo.name = "TestApp2"
        mockApplicationInfo.minSdkVersion = 21
        mockApplicationInfo.targetSdkVersion = 33

        val mockApplication = mockk<Application>(relaxed = true)
        every { mockApplication.applicationContext } returns mockContext

        NeuroID.getInternalInstance()?.application = mockApplication
        // Don't set rnVersion - should use default empty string

        // Call captureApplicationMetaData
        val mockNIDSharedPrefsDefaults = mockk<NIDSharedPrefsDefaults>()
        every{mockNIDSharedPrefsDefaults.getPlatform()}  returns "Android"
        NeuroID.getInternalInstance()?.sharedPrefsDefaults = mockNIDSharedPrefsDefaults
        NeuroID.getInternalInstance()?.captureApplicationMetaData()

        // Verify event was captured
        assertEquals(1, storedEvents.count())
        val event = storedEvents.firstOrNull()
        assertEquals(APPLICATION_METADATA, event?.type)

        // Verify the attrs contain the new parameters with defaults
        val attrs = event?.attrs
        assert(attrs != null)

        // Check for hostRNVersion (should be empty string by default)
        val hostRNVersionAttr = attrs?.find { it["n"] == "rnVersion" }
        assertEquals("", hostRNVersionAttr?.get("v"))
    }

    //    captureEvent

    // ── getSDKVersion ────────────────────────────────────────────────────────

    @Test
    fun test_getSDKVersion_returnsNonEmptyString() {
        val version = NeuroID.getInstance()?.getSDKVersion()
        assert(!version.isNullOrEmpty()) { "getSDKVersion() should return a non-empty string" }
    }

    @Test
    fun test_getSDKVersion_matchesNIDVersionUtil() {
        // The NeuroID implementation delegates directly to NIDVersion.getSDKVersion(),
        // so the two values must be identical.
        val expected = NIDVersion.getSDKVersion()
        val actual = NeuroID.getInstance()?.getSDKVersion()
        assertEquals(expected, actual)
    }

    @Test
    fun test_getSDKVersion_androidFlavour_doesNotContainRnSuffix() {
        // In the android flavour the version string must NOT have "-rn"
        val mockedBuildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { mockedBuildConfigWrapper.getFlavor() } returns "android"
        every { mockedBuildConfigWrapper.getBuildVersion() } returns "4.0.0"
        every { mockedBuildConfigWrapper.getGitHash() } returns "abc1234"

        val version = NIDVersion.getSDKVersion(mockedBuildConfigWrapper)
        assert(!version.contains("-rn")) { "Android flavour should not contain '-rn', got: $version" }
        assert(version.contains("android")) { "Version should contain 'android', got: $version" }
    }

    // ── stopSession ──────────────────────────────────────────────────────────

    @Test
    fun test_stopSession_delegatesToSessionService_returnsTrue() {
        val mockedSessionService = getMockedSessionService()
        every { mockedSessionService.stopSession() } returns true
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        val result = NeuroID.getInstance()?.stopSession()

        verify(exactly = 1) { mockedSessionService.stopSession() }
        assertEquals(true, result)
    }

    @Test
    fun test_stopSession_delegatesToSessionService_returnsFalse() {
        val mockedSessionService = getMockedSessionService()
        every { mockedSessionService.stopSession() } returns false
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        val result = NeuroID.getInstance()?.stopSession()

        verify(exactly = 1) { mockedSessionService.stopSession() }
        assertEquals(false, result)
    }

    // ── pauseCollection ───────────────────────────────────────────────────────

    @Test
    fun test_pauseCollection_delegatesToSessionService() {
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        NeuroID.getInstance()?.pauseCollection()

        // pauseCollection() always calls sessionService.pauseCollection(true)
        verify(exactly = 1) { mockedSessionService.pauseCollection(true) }
    }

    @Test
    fun test_pauseCollection_whenSDKNotStarted_stillDelegates() {
        NeuroID._isSDKStarted = false
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        NeuroID.getInstance()?.pauseCollection()

        verify(exactly = 1) { mockedSessionService.pauseCollection(true) }
    }

    // ── resumeCollection ──────────────────────────────────────────────────────

    @Test
    fun test_resumeCollection_delegatesToSessionService() {
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        NeuroID.getInstance()?.resumeCollection()

        verify(exactly = 1) { mockedSessionService.resumeCollection() }
    }

    @Test
    fun test_resumeCollection_whenSDKNotStarted_stillDelegates() {
        NeuroID._isSDKStarted = false
        val mockedSessionService = getMockedSessionService()
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        NeuroID.getInstance()?.resumeCollection()

        verify(exactly = 1) { mockedSessionService.resumeCollection() }
    }

    // ── startSession ──────────────────────────────────────────────────────────

    @Test
    fun test_startSession_withSessionID_completionInvokedWithSuccess() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "session-abc")
        every { mockedSessionService.startSession(null, "session-abc", any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startSession(sessionID = "session-abc") { result ->
            completionResult = result
        }

        verify(exactly = 1) { mockedSessionService.startSession(null, "session-abc", any()) }
        assertEquals(expectedResult, completionResult)
    }

    @Test
    fun test_startSession_withNullSessionID_completionInvoked() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "generated-id")
        every { mockedSessionService.startSession(null, null, any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startSession(sessionID = null) { result ->
            completionResult = result
        }

        verify(exactly = 1) { mockedSessionService.startSession(null, null, any()) }
        assertEquals(expectedResult, completionResult)
    }

    @Test
    fun test_startSession_failure_completionInvokedWithFalse() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = false, sessionID = "")
        every { mockedSessionService.startSession(null, any(), any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startSession { result ->
            completionResult = result
        }

        assertEquals(false, completionResult?.started)
    }

    // ── startAppFlow ──────────────────────────────────────────────────────────

    @Test
    fun test_startAppFlow_withUserID_completionInvokedWithSuccess() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "flow-session-1")
        every { mockedSessionService.startAppFlow("site-123", "user-abc", any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startAppFlow(siteID = "site-123", userID = "user-abc") { result ->
            completionResult = result
        }

        verify(exactly = 1) { mockedSessionService.startAppFlow("site-123", "user-abc", any()) }
        assertEquals(expectedResult, completionResult)
    }

    @Test
    fun test_startAppFlow_withNullUserID_completionInvoked() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "flow-session-2")
        every { mockedSessionService.startAppFlow("site-xyz", null, any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startAppFlow(siteID = "site-xyz") { result ->
            completionResult = result
        }

        verify(exactly = 1) { mockedSessionService.startAppFlow("site-xyz", null, any()) }
        assertEquals(expectedResult, completionResult)
    }

    @Test
    fun test_startAppFlow_failure_completionInvokedWithFalse() {
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = false, sessionID = "")
        every { mockedSessionService.startAppFlow(any(), any(), any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        var completionResult: SessionStartResult? = null
        NeuroID.getInstance()?.startAppFlow(siteID = "site-fail") { result ->
            completionResult = result
        }

        assertEquals(false, completionResult?.started)
    }

    // ── Default-parameter overloads (cover the 5 missing NeuroIDPublic methods) ──

    @Test
    fun test_start_defaultCompletion_doesNotThrow() {
        // Exercises: start(completion = {}) — the no-arg default overload
        val mockedSessionService = getMockedSessionService()
        every { mockedSessionService.start(siteID = null, completion = any()) } answers {
            val completion = secondArg<(Boolean) -> Unit>()
            completion(true)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService
        NeuroID._isSDKStarted = false

        // Call with NO arguments — exercises the synthesised default parameter path
        NeuroID.getInstance()?.start()

        verify(exactly = 1) { mockedSessionService.start(siteID = null, completion = any()) }
    }

    @Test
    fun test_startSession_noArgs_defaultCompletion() {
        // Exercises: startSession() — both sessionID and completion use defaults
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "auto-id")
        every { mockedSessionService.startSession(null, null, any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        // No arguments at all
        NeuroID.getInstance()?.startSession()

        verify(exactly = 1) { mockedSessionService.startSession(null, null, any()) }
    }

    @Test
    fun test_startSession_withSessionID_defaultCompletion() {
        // Exercises: startSession(sessionID = "id") — completion uses default {}
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "given-id")
        every { mockedSessionService.startSession(null, "given-id", any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        // Provide sessionID but omit completion
        NeuroID.getInstance()?.startSession(sessionID = "given-id")

        verify(exactly = 1) { mockedSessionService.startSession(null, "given-id", any()) }
    }

    @Test
    fun test_attemptedLogin_noArgs_defaultNullUserId() {
        // Exercises: attemptedLogin() — the no-arg default overload (userId = null)
        setupAttemptedLoginTestEnvironment(false)
        val mockIdentificationService = NeuroID.getInternalInstance()?.identifierService

        // Call WITHOUT arguments — covers the default parameter synthetic path
        val result = NeuroID.getInstance()?.attemptedLogin()

        verify {
            mockIdentificationService?.setGenericUserID(
                any(),
                "ATTEMPTED_LOGIN",
                "scrubbed-id-failed-validation",
                false
            )
        }
        assertEquals(true, result)
    }

    @Test
    fun test_startAppFlow_withSiteIDOnly_defaultCompletion() {
        // Exercises: startAppFlow(siteID = "site") — userID and completion use defaults
        val mockedSessionService = getMockedSessionService()
        val expectedResult = SessionStartResult(started = true, sessionID = "flow-default")
        every { mockedSessionService.startAppFlow("site-default", null, any()) } answers {
            val completion = thirdArg<(SessionStartResult) -> Unit>()
            completion(expectedResult)
        }
        NeuroID.getInternalInstance()?.sessionService = mockedSessionService

        // Provide only siteID; omit userID and completion
        NeuroID.getInstance()?.startAppFlow(siteID = "site-default")

        verify(exactly = 1) { mockedSessionService.startAppFlow("site-default", null, any()) }
    }
}
