package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.NetworkInfo
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.events.APPLICATION_SUBMIT
import com.neuroid.tracker.events.APPLICATION_METADATA
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.models.NIDConfiguration
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
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
                false,
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
        NeuroID.getInternalInstance()?.resetClientId()

        // Verify clientID was updated
        assertEquals(newClientID, NeuroID.getInternalInstance()?.clientID)
        assert(originalClientID != NeuroID.getInternalInstance()?.clientID)

        // Verify resetClientID was called
        verify(exactly = 1) {
            anyConstructed<NIDSharedPrefsDefaults>().resetClientID()
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

    // getUserId() Tests (deprecated method)
    @Test
    fun test_getUserId_returnsUserID() {
        val testUserID = "test-user-123"
        val mockedIdentifierService = getMockedIdentifierService()
        NeuroID.getInternalInstance()?.identifierService = mockedIdentifierService

        // Mock getUserID to return test user ID
        every { mockedIdentifierService.getUserID(any()) } returns testUserID

        @Suppress("DEPRECATION")
        val result = NeuroID.getInstance()?.getUserId()

        // Verify identifierService.getUserID was called
        verify(exactly = 1) {
            mockedIdentifierService.getUserID(any())
        }

        // Verify result matches
        assertEquals(testUserID, result)
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

    //    setEnvironment - DEPRECATED
    @Test
    fun testSetEnvironment() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironment"))

        NeuroID.environment = ""

        NeuroID.getInstance()?.setEnvironment("MYENV")

        assertEquals("", NeuroID.environment)
        assertInfoCount(1)
    }

    //    setEnvironmentProduction - DEPRECATED
    @Test
    fun testSetEnvironmentProduction_true() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NeuroID.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(true)

        assertEquals("", NeuroID.environment)
        assertInfoCount(1)
    }

    @Test
    fun testSetEnvironmentProduction_false() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NeuroID.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(false)

        assertEquals("", NeuroID.environment)
        assertInfoCount(1)
    }

    //    getEnvironment - DEPRECATED
    @Test
    fun testGetEnvironment() {
        val expectedValue = "MyEnv"
        NeuroID.environment = expectedValue

        val value = NeuroID.getInstance()?.getEnvironment()

        assertEquals(expectedValue, value)
    }

    //    setSiteId - DEPRECATED
    @Test
    fun testSetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setSiteId"))

        val expectedValue = "TestSiteId"
        NeuroID.siteID = "DifferentSiteID"

        NeuroID.getInstance()?.setSiteId(expectedValue)

        assertEquals(expectedValue, NeuroID.siteID)
        assertInfoCount(1)
    }

    //    getSiteId - DEPRECATED
    @Test
    fun testGetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("getSiteId"))

        val expectedValue = ""
        NeuroID.siteID = "TestSiteId"

        val value = NeuroID.getInternalInstance()?.getSiteId()

        assertEquals(expectedValue, value)
        assertInfoCount(1)
    }

    //    getSessionId
    @Test
    fun testGetSessionID() {
        val expectedValue = "testSessionID"
        NeuroID.getInternalInstance()?.sessionID = expectedValue

        val value = NeuroID.getInternalInstance()?.getSessionID()

        assertEquals(expectedValue, value)
    }

    //    getClientId
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

    //    formSubmit - Deprecated
    @Test
    fun testFormSubmit() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmit"))

        NeuroID.getInstance()?.formSubmit()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === APPLICATION_SUBMIT)
    }

    //    formSubmitSuccess - Deprecated
    @Test
    fun testFormSubmitSuccess() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmitSuccess"))

        NeuroID.getInstance()?.formSubmitSuccess()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === FORM_SUBMIT_SUCCESS)
    }

    //    formSubmitFailure - Deprecated
    @Test
    fun testFormSubmitFailure() {
        NeuroID._isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmitFailure"))

        NeuroID.getInstance()?.formSubmitFailure()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === FORM_SUBMIT_FAILURE)
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

        NeuroID.getInstance()?.setIsRN()

        assertEquals(true, NeuroID.getInternalInstance()?.isRN)
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
        NeuroID.getInternalInstance()?.hostReactNativeVersion = "0.72.0"

        // Call captureApplicationMetaData
        NeuroID.getInternalInstance()?.captureApplicationMetaData()

        // Verify event was captured
        assertEquals(1, storedEvents.count())
        val event = storedEvents.firstOrNull()
        assertEquals(APPLICATION_METADATA, event?.type)

        // Verify the attrs contain the new parameters
        val attrs = event?.attrs
        assert(attrs != null)
        assert(attrs!!.isNotEmpty())

        // Check for hostRNVersion
        val hostRNVersionAttr = attrs.find { it["n"] == "hostRNVersion" }
        assertEquals("0.72.0", hostRNVersionAttr?.get("v"))

        // Check for hostMinSDKLevel
        val hostMinSDKLevelAttr = attrs.find { it["n"] == "hostMinSDKLevel" }
        assertEquals(24, hostMinSDKLevelAttr?.get("v"))

        // Check for hostTargetSDKLevel
        val hostTargetSDKLevelAttr = attrs.find { it["n"] == "hostTargetSDKLevel" }
        assertEquals(34, hostTargetSDKLevelAttr?.get("v"))

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
        // Don't set hostReactNativeVersion - should use default empty string

        // Call captureApplicationMetaData
        NeuroID.getInternalInstance()?.captureApplicationMetaData()

        // Verify event was captured
        assertEquals(1, storedEvents.count())
        val event = storedEvents.firstOrNull()
        assertEquals(APPLICATION_METADATA, event?.type)

        // Verify the attrs contain the new parameters with defaults
        val attrs = event?.attrs
        assert(attrs != null)

        // Check for hostRNVersion (should be empty string by default)
        val hostRNVersionAttr = attrs?.find { it["n"] == "hostRNVersion" }
        assertEquals("", hostRNVersionAttr?.get("v"))
    }

    //    captureEvent
}
