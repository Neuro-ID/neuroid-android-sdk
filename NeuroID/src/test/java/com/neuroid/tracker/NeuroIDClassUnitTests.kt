package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.events.APPLICATION_SUBMIT
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Job

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals


enum class TestLogLevel {
    DEBUG,
    INFO,
    ERROR
}

class NeuroIDClassUnitTests {
    private var errorCount = 0
    private var infoCount = 0
    private var debugCount = 0

    // datastoreMock vars
    private var storedEvents = mutableSetOf<NIDEventModel>()
    private var queuedEvents = mutableSetOf<NIDEventModel>()
    private var excludedIds = mutableSetOf<String>()

    @MockK
    lateinit var mockedApplication: Application
    private lateinit var mockContext: Context

    private fun assertLogMessage(type: TestLogLevel, expectedMessage: String, actualMessage: Any?) {
        if (actualMessage != "" && actualMessage != null) {
            assertEquals(expectedMessage, actualMessage)
        }

        when (type) {
            TestLogLevel.DEBUG -> debugCount += 1
            TestLogLevel.INFO -> infoCount += 1
            TestLogLevel.ERROR -> errorCount += 1
        }

        return
    }

    // Helper Functions
    private fun setNeuroIDInstance() {
        val neuroId = NeuroID.Builder(
            null,
            "key_test_fake1234"
        ).build()
        NeuroID.setNeuroIdInstance(neuroId)
    }

    private fun setNeuroIDMockedLogger(
        errorMessage: String = "",
        infoMessage: String = "",
        debugMessage: String = ""
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

        NeuroID.getInstance()?.setLoggerInstance(log)
    }

    private fun setMockedDataStore() {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } answers {
            storedEvents.add(args[0] as NIDEventModel)
            mockk<Job>()
        }

        every { dataStoreManager.queueEvent(any()) } answers {
            queuedEvents.add(args[0] as NIDEventModel)
        }

        every { dataStoreManager.addViewIdExclude(any()) } answers {
            excludedIds.add(args[0] as String)
        }

        every { dataStoreManager.saveAndClearAllQueuedEvents() } answers {

        }

        NeuroID.getInstance()?.setDataStoreInstance(dataStoreManager)
    }

    private fun setMockedApplication() {
        val mockedApplication = mockk<Application>()

        every { mockedApplication.applicationContext } answers {
            mockk<Context>()
        }

        NeuroID.getInstance()?.application = mockedApplication
    }

    private fun clearLogCounts() {
        errorCount = 0
        infoCount = 0
        debugCount = 0
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

    @Before
    fun setUp() {
        // setup instance and logging
        setNeuroIDInstance()
        NeuroID.getInstance()?.application = null
        setNeuroIDMockedLogger()

        clearLogCounts()
        storedEvents.clear()
        queuedEvents.clear()
        excludedIds.clear()
    }

    @After
    fun tearDown() {
        assertEquals("Expected Log Error Count is Greater than 0", 0, errorCount)
        assertEquals("Expected Log Info Count is Greater than 0", 0, infoCount)
        assertEquals("Expected Log Debug Count is Greater than 0", 0, debugCount)
    }

    // Function Tests

    //    setLoggerInstance - Used for mocking
    //    setDataStoreInstance - Used for mocking
    //    setNIDActivityCallbackInstance - Used for mocking
    //    setNIDJobServiceManager - Used for mocking

    //    validateClientKey
    @Test
    fun testValidateClientKey_bad_key() {
        val value = NeuroID.getInstance()?.validateClientKey("kjjhgh")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_invalid_key() {
        val value = NeuroID.getInstance()?.validateClientKey("key_tert_fdffsd")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_valid_key() {
        val value = NeuroID.getInstance()?.validateClientKey("key_test_1235")

        assertEquals(true, value)
    }

    //    validateUserId
    @Test
    fun testValidateUserID_valid() {
        val value = NeuroID.getInstance()?.validateUserId("goodUserId")

        assertEquals(true, value)
    }

    @Test
    fun testValidateUserID_invalid() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")
        val value = NeuroID.getInstance()?.validateUserId("bad userID")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    setUserID
    @Test
    fun testSetUserID_success_notStarted() {
        setMockedDataStore()
        NeuroID.isSDKStarted = false

        val value = NeuroID.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
        assertEquals(1, queuedEvents.count())
        assertEquals(true, queuedEvents.firstOrNull()?.type === SET_USER_ID)
    }

    @Test
    fun testSetUserID_success_Started() {
        setMockedDataStore()
        NeuroID.isSDKStarted = true

        val value = NeuroID.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === SET_USER_ID)
    }

    @Test
    fun testSetUserID_failure() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        val value = NeuroID.getInstance()?.setUserID("Bad UserID")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    getUserId
    @Test
    fun testGetUserID() {
        val expectedValue = "testID"
        NeuroID.getInstance()?.userID = expectedValue
        val value = NeuroID.getInstance()?.getUserID()

        assertEquals(expectedValue, value)
    }

    //    setScreenName
    @Test
    fun testSetScreenName_success() {
        NeuroID.isSDKStarted = true

        val value = NeuroID.getInstance()?.setScreenName("testName")

        assertEquals(true, value)
    }

    @Test
    fun testSetScreenName_failure() {
        setNeuroIDMockedLogger(errorMessage = "NeuroID SDK is not started")
        NeuroID.isSDKStarted = false

        val value = NeuroID.getInstance()?.setScreenName("testName")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    getScreenName
    @Test
    fun testGetScreenName() {
        val expectedValue = "myScreen"
        NIDServiceTracker.screenName = expectedValue

        val value = NeuroID.getInstance()?.getScreenName()

        assertEquals(expectedValue, value)
    }

    //    excludeViewByTestID
    @Test
    fun testExcludeViewByTestID() {
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInstance()?.excludeViewByTestID("fddf")

        assertEquals(1, excludedIds.count())
    }

    //    setEnvironment - DEPRECATED
    @Test
    fun testSetEnvironment() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironment"))

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironment("MYENV")

        assertEquals("", NIDServiceTracker.environment)
        assertInfoCount(1)
    }

    //    setEnvironmentProduction - DEPRECATED
    @Test
    fun testSetEnvironmentProduction_true() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(true)

        assertEquals("", NIDServiceTracker.environment)
        assertInfoCount(1)
    }

    @Test
    fun testSetEnvironmentProduction_false() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(false)

        assertEquals("", NIDServiceTracker.environment)
        assertInfoCount(1)
    }

    //    getEnvironment - DEPRECATED
    @Test
    fun testGetEnvironment() {

        val expectedValue = "MyEnv"
        NIDServiceTracker.environment = expectedValue


        val value = NeuroID.getInstance()?.getEnvironment()

        assertEquals(expectedValue, value)
    }

    //    setSiteId - DEPRECATED
    @Test
    fun testSetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setSiteId"))

        val expectedValue = "TestSiteId"
        NIDServiceTracker.siteId = "DifferentSiteID"

        NeuroID.getInstance()?.setSiteId(expectedValue)

        assertEquals(expectedValue, NIDServiceTracker.siteId)
        assertInfoCount(1)
    }

    //    getSiteId - DEPRECATED
    @Test
    fun testGetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("getSiteId"))

        val expectedValue = ""
        NIDServiceTracker.siteId = "TestSiteId"

        val value = NeuroID.getInstance()?.getSiteId()

        assertEquals(expectedValue, value)
        assertInfoCount(1)
    }

    //    getSessionId
    @Test
    fun testGetSessionID() {

        val expectedValue = "testSessionID"
        NeuroID.getInstance()?.sessionID = expectedValue

        val value = NeuroID.getInstance()?.getSessionID()

        assertEquals(expectedValue, value)
    }

    //    getClientId
    @Test
    fun testGetClientID() {

        val expectedValue = "testClientID"
        NeuroID.getInstance()?.clientID = expectedValue

        val value = NeuroID.getInstance()?.getClientID()

        assertEquals(expectedValue, value)
    }

    //    getForceStart
    @Test
    fun testGetForceStart_false() {

        val expectedValue = false

        val value = NeuroID.getInstance()?.getForceStart()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testGetForceStart_true() {

        val expectedValue = true
        NeuroID.getInstance()?.forceStart = expectedValue

        val value = NeuroID.getInstance()?.getForceStart()

        assertEquals(expectedValue, value)
    }

    //    registerPageTargets
    @Test
    fun testRegisterPageTargets() {
        val mockedNIDACB = mockk<NIDActivityCallbacks>()
        every { mockedNIDACB.forceStart(any()) } just runs
        NeuroID.getInstance()?.setNIDActivityCallbackInstance(mockedNIDACB)

        val mockedActivity = mockk<Activity>()

        NeuroID.getInstance()?.registerPageTargets(mockedActivity)

        assertEquals(true, NeuroID.getInstance()?.forceStart)
        verify { mockedNIDACB.forceStart(mockedActivity) }

        // reset for other tests
        NeuroID.getInstance()?.forceStart = false
    }

    //    getTabId
    @Test
    fun testGetTabId() {
        val expectedValue = "MyRNDID"

        NIDServiceTracker.rndmId = expectedValue

        val value = NeuroID.getInstance()?.getTabId()

        assertEquals(expectedValue, value)
    }

    //    getFirstTS - not worth testing
    @Test
    fun testGetFirstTS() {
        val expectedValue: Long = 1234

        NeuroID.getInstance()?.timestamp = expectedValue

        val value = NeuroID.getInstance()?.getFirstTS()

        assertEquals(expectedValue, value)
    }

//    getJsonPayLoad - not sure how to mock
//    resetJsonPayLoad - not sure how to mock

    //    captureEvent - not sure how to mock Application context
    @Test
    fun testCaptureEvent() {
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInstance()?.captureEvent("testEvent", "testTGS")

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === "testEvent")
    }

    //    formSubmit - Deprecated
    @Test
    fun testFormSubmit() {
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
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmitFailure"))

        NeuroID.getInstance()?.formSubmitFailure()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === FORM_SUBMIT_FAILURE)
    }

    //    start
    @Test
    fun testStart_success() {
        NeuroID.isSDKStarted = false

        val value = NeuroID.getInstance()?.start()

        assertEquals(true, value)
        assertEquals(true, NeuroID.isSDKStarted)
    }

    @Test
    fun testStart_failure() {
        setNeuroIDMockedLogger(
            errorMessage = "Missing Client Key - please call configure prior to calling start"
        )

        NeuroID.isSDKStarted = false

        NeuroID.getInstance()?.clientKey = ""

        val value = NeuroID.getInstance()?.start()

        assertEquals("", NeuroID.getInstance()?.clientKey)
        assertEquals(false, value)
        assertEquals(false, NeuroID.isSDKStarted)

        assertErrorCount(1)
    }

    //    stop
    @Test
    fun testStop() {
        NeuroID.isSDKStarted = true

        NeuroID.getInstance()?.stop()

        assertEquals(false, NeuroID.isSDKStarted)
    }

//    closeSession - Need to mock NIDJobServiceManager
//    resetClientId - Need to mock Application & Shared Preferences

    //    isStopped
    @Test
    fun testIsStopped_true() {
        val expectedValue = true
        NeuroID.isSDKStarted = !expectedValue

        val value = NeuroID.getInstance()?.isStopped()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testIsStopped_false() {
        val expectedValue = false
        NeuroID.isSDKStarted = !expectedValue

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
        NeuroID.getInstance()?.application = mockedApplication


        var value = NeuroID.getInstance()?.getApplicationContext()
        assertEquals(mockedContext, value)
    }

//    createSession - Need to mock Application
//    createMobileMetadata - Need to mock Application

    //    setIsRN
    @Test
    fun testSetIsRN() {
        NeuroID.getInstance()?.isRN = false
        assertEquals(false, NeuroID.getInstance()?.isRN)

        NeuroID.getInstance()?.setIsRN()

        assertEquals(true, NeuroID.getInstance()?.isRN)
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

    //    getSDKVersion
    @Test
    fun testGetSDKVersion_notRN() {
        NeuroID.getInstance()?.isRN = false

        val version = NeuroID.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-"))
        assertEquals(false, version?.contains("rn-"))
    }

    @Test
    fun testGetSDKVersion_RN() {
        NeuroID.getInstance()?.isRN = true

        val version = NeuroID.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-rn"))
    }

    //    clearSessionVariables
    @Test
    fun testClearSessionVariables() {
        NeuroID.getInstance()?.userID = "myID"

        NeuroID.getInstance()?.clearSessionVariables()

        assertEquals("", NeuroID.getInstance()?.userID)
    }

    //    startSession
    @Test
    fun testStartSession_success_no_id() {
        setMockedDataStore()
        NeuroID.getInstance()?.let {
            it.clientKey = "dummyKey"
            val (started, id) = it.startSession()
            assertEquals(true, started)
        }
    }

    @Test
    fun testStartSession_success_id() {
        setMockedDataStore()
        NeuroID.getInstance()?.let {
            it.clientKey = "dummyKey"
            val (started, id) = it.startSession("testID")
            assertEquals(true, started)
            assertEquals("testID", id)
        }
    }

    @Test
    fun testStartSession_failure_clientKey() {
        setNeuroIDMockedLogger(
            errorMessage = "Missing Client Key - please call configure prior to calling start"
        )
        NeuroID.getInstance()?.let {
            it.clientKey = ""
            val (started, id) = it.startSession()
            assertEquals(false, started)
            assertEquals("", id)

            assertErrorCount(1)
        }
    }

    @Test
    fun testStartSession_failure_userID() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")
        NeuroID.getInstance()?.let {
            it.clientKey = "dummyKey"
            val (started, id) = it.startSession("bad user 343%%^")
            assertEquals(false, started)
            assertEquals("", id)

            assertErrorCount(1)
        }
    }

    //    pauseCollection
    @Test
    fun testPauseCollection() {
        NeuroID.getInstance()?.let {
            it.pauseCollection()
            assertEquals(false, NeuroID.isSDKStarted)
        }
    }

    //    resumeCollection
    fun testResumeCollection() {
        NeuroID.getInstance()?.let {
            it.resumeCollection()
            assertEquals(true, NeuroID.isSDKStarted)
        }
    }

    //    stopSession
    fun testStopSession() {
        NeuroID.getInstance()?.let {
            val stopped = it.stopSession()
            assertEquals(true, stopped)
            assertEquals(false, NeuroID.isSDKStarted)
        }
    }


}