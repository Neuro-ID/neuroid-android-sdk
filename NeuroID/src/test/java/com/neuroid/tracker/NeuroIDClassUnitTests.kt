package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.events.APPLICATION_SUBMIT
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
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
        // setup instance and logging
        setNeuroIDInstance()
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

    // Class Init Test
    @Test
    fun test_configure_tab_id() {
        setMockedNIDJobServiceManager(false)
        val ogTabID = NeuroID.getInternalInstance()?.tabID

        NeuroID.getInternalInstance()?.resetSingletonInstance()

        assert(ogTabID != NeuroID.getInternalInstance()?.tabID)
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
        every { mockIdentificationService.setGenericUserID(any(), any(), any()) } returns validID
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
            mockIdentificationService?.setGenericUserID(any(), userId ?: any(), any())
        }

        if (expectedFailedResult) {
            dataStoreManager?.saveEvent(NIDEventModel(ts = 1, type = "ATTEMPTED_LOGIN", uid = expectedUserId))
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
        testAttemptedLogin("¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂ", "scrubbed-id-failed-validation", true)
        testAttemptedLogin("ÃÄÅÆÇÈÉ ÊË Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö", "scrubbed-id-failed-validation", true)
        testAttemptedLogin("almost good", "scrubbed-id-failed-validation", true)
    }

    @Test
    fun testSetTestURL() {
        NeuroID.getInstance()?.setTestURL("myTests")

        assertEquals(true, NeuroID.endpoint == "myTests")
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

    fun setMockedEmptyLogger(): NIDLogWrapper {
        val logger = getMockedLogger()
        NeuroID.getInternalInstance()?.logger = logger
        return logger
    }
}
