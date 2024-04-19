package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import com.neuroid.tracker.callbacks.ActivityCallbacks
import com.neuroid.tracker.events.APPLICATION_SUBMIT
import com.neuroid.tracker.events.FORM_SUBMIT_FAILURE
import com.neuroid.tracker.events.FORM_SUBMIT_SUCCESS
import com.neuroid.tracker.events.SET_REGISTERED_USER_ID
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.events.NID_ORIGIN_CODE_CUSTOMER
import com.neuroid.tracker.events.NID_ORIGIN_CODE_FAIL
import com.neuroid.tracker.events.NID_ORIGIN_CODE_NID
import com.neuroid.tracker.events.NID_ORIGIN_CUSTOMER_SET
import com.neuroid.tracker.events.NID_ORIGIN_NID_SET
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.*
import kotlinx.coroutines.Job

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue


enum class TestLogLevel {
    DEBUG,
    INFO,
    ERROR,
    WARNING
}

open class NeuroIDClassUnitTests {
    private var errorCount = 0
    private var infoCount = 0
    private var debugCount = 0
    private var warningCount = 0

    // datastoreMock vars
    private var storedEvents = mutableSetOf<NIDEventModel>()
    private var queuedEvents = mutableSetOf<NIDEventModel>()

    private fun assertLogMessage(type: TestLogLevel, expectedMessage: String, actualMessage: Any?) {
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
        NeuroIDImpl.Builder(null, "key_test_fake1234", NeuroIDImpl.DEVELOPMENT).build()
    }

    private fun setNeuroIDMockedLogger(
        errorMessage: String = "",
        infoMessage: String = "",
        debugMessage: String = "",
        warningMessage: String = ""
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

        NeuroIDImpl.getInternalInstance()?.setLoggerInstance(log)
    }

    fun setMockedDataStore(fullBuffer:Boolean = false):NIDDataStoreManager {
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

        NeuroIDImpl.getInternalInstance()?.setDataStoreInstance(dataStoreManager)

        return dataStoreManager
    }

    private fun setMockedApplication() {
        val mockedApplication = mockk<Application>()

        every { mockedApplication.applicationContext } answers {
            mockk<Context>()
        }

        NeuroIDImpl.getInternalInstance()?.application = mockedApplication
    }

    private fun setMockedNIDJobServiceManager(isStopped:Boolean = true) {
        val mockedNIDJobServiceManager = mockk<NIDJobServiceManager>()

        every { mockedNIDJobServiceManager.startJob(any(), any()) } just runs
        every { mockedNIDJobServiceManager.isStopped() } returns isStopped
        every { mockedNIDJobServiceManager.stopJob() } just runs

        coEvery {
            mockedNIDJobServiceManager.sendEvents(any())
        } just runs

        NeuroIDImpl.getInternalInstance()?.setNIDJobServiceManager(mockedNIDJobServiceManager)
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
        NeuroIDImpl.getInternalInstance()?.setLoggerInstance(log)
    }

    @Before
    fun setUp() {
        // setup instance and logging
        setNeuroIDInstance()
        NeuroIDImpl.getInternalInstance()?.application = null
        setNeuroIDMockedLogger()

        clearLogCounts()
        storedEvents.clear()
        queuedEvents.clear()
        NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.clear()
    }

    @After
    fun tearDown() {
        assertEquals("Expected Log Error Count is Greater than 0", 0, errorCount)
        assertEquals("Expected Log Info Count is Greater than 0", 0, infoCount)
        assertEquals("Expected Log Debug Count is Greater than 0", 0, debugCount)
        assertEquals("Expected Log Warning Count is Greater than 0", 0, warningCount)
        unmockkAll()
    }

    // Function Tests

    //    setLoggerInstance - Used for mocking
    //    setDataStoreInstance - Used for mocking
    //    setNIDActivityCallbackInstance - Used for mocking
    //    setNIDJobServiceManager - Used for mocking

    //   setTestURL
    @Test
    fun testSetTestURL() {
        NeuroIDImpl.getInstance()?.setTestURL("myTests")

        assertEquals(true, NeuroIDImpl.endpoint == "myTests")
    }

    //   setTestingNeuroIDDevURL
    @Test
    fun testSetTestingNeuroIDDevURL() {
        NeuroIDImpl.getInstance()?.setTestingNeuroIDDevURL()

        assertEquals(true, NeuroIDImpl.endpoint == Constants.devEndpoint.displayName)
    }

    //    validateClientKey
    @Test
    fun testValidateClientKey_bad_key() {
        val value = NeuroIDImpl.getInternalInstance()?.validateClientKey("kjjhgh")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_invalid_key() {
        val value = NeuroIDImpl.getInternalInstance()?.validateClientKey("key_tert_fdffsd")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_valid_key() {
        val value = NeuroIDImpl.getInternalInstance()?.validateClientKey("key_test_1235")

        assertEquals(true, value)
    }

    //    validateUserId
    @Test
    fun testValidateUserID_valid() {
        val value = NeuroIDImpl.getInternalInstance()?.validateUserId("goodUserId")

        assertEquals(true, value)
    }

    @Test
    fun testValidateUserID_invalid() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")
        val value = NeuroIDImpl.getInternalInstance()?.validateUserId("bad userID")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    setUserID
    @Test
    fun testSetUserID_success_notStarted() {
        setMockedDataStore()
        NeuroIDImpl.isSDKStarted = false

        val value = NeuroIDImpl.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
        assertEquals(4, queuedEvents.count())
        assertEquals(1, queuedEvents.count{it.type === SET_USER_ID})
        assertEquals(3, queuedEvents.count { it.type === SET_VARIABLE  })
    }

    @Test
    fun testSetRegisteredUserID_success_notStarted() {
        setMockedDataStore()
        NeuroIDImpl.isSDKStarted = false

        val value = NeuroIDImpl.getInstance()?.setRegisteredUserID("myUserID")

        assertEquals(true, value)
        assertEquals(4, queuedEvents.count())
        assertEquals(1, queuedEvents.count{it.type === SET_REGISTERED_USER_ID})
        assertEquals(3, queuedEvents.count { it.type === SET_VARIABLE  })
    }

    @Test
    fun testUserID_success_Started() {
        setMockedDataStore()
        setNeuroIDMockedLogger()
        setMockedNIDJobServiceManager(false)

        NeuroIDImpl.isSDKStarted = true

        val value = NeuroIDImpl.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
        assertEquals(4, storedEvents.count())
        assertEquals(3, storedEvents.count { x -> x.type == SET_VARIABLE })
        assertEquals(1, storedEvents.count { x -> x.type === SET_USER_ID })
    }

    @Test
    fun testRegisteredUserID_success_Started() {
        setMockedDataStore()
        setNeuroIDMockedLogger()
        setMockedNIDJobServiceManager(false)

        NeuroIDImpl.isSDKStarted = true

        val value = NeuroIDImpl.getInstance()?.setRegisteredUserID("myRegisteredUserID")

        assertEquals(true, value)
        assertEquals(4, storedEvents.count())
        assertEquals(3, storedEvents.count { x -> x.type == SET_VARIABLE })
        assertEquals(1, storedEvents.count { x -> x.type === SET_REGISTERED_USER_ID })
    }

    @Test
    fun testSetUserID_failure() {
        setMockedDataStore(false)
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        val value = NeuroIDImpl.getInstance()?.setUserID("Bad UserID")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    @Test
    fun testSetRegisteredUserID_failure() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        val value = NeuroIDImpl.getInstance()?.setRegisteredUserID("Bad User REGISTERED ID")

        assertEquals(false, value)
        assertErrorCount(1)
        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testSetRegisteredUserID_failure_Started() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        NeuroIDImpl.isSDKStarted = true

        val value = NeuroIDImpl.getInstance()?.setRegisteredUserID("Bad User REGISTERED ID")

        assertEquals(false, value)
        assertErrorCount(1)
        assertEquals(0, storedEvents.count())
    }

    //    getUserId
    @Test
    fun testGetUserID() {
        val expectedValue = "testID"
        NeuroIDImpl.getInternalInstance()?.userID = expectedValue
        val value = NeuroIDImpl.getInstance()?.getUserID()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testGetRegisteredUserID() {
        val expectedValue = "testRegisteredID"
        NeuroIDImpl.getInternalInstance()?.registeredUserID = expectedValue
        val value = NeuroIDImpl.getInstance()?.getRegisteredUserID()

        assertEquals(expectedValue, value)
    }

    //    setScreenName
    @Test
    fun testSetScreenName_success() {
        NeuroIDImpl.isSDKStarted = true

        val value = NeuroIDImpl.getInstance()?.setScreenName("testName")

        assertEquals(true, value)
    }

    @Test
    fun testSetScreenName_failure() {
        setNeuroIDMockedLogger(errorMessage = "NeuroID SDK is not started")
        NeuroIDImpl.isSDKStarted = false

        val value = NeuroIDImpl.getInstance()?.setScreenName("testName")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    //    getScreenName
    @Test
    fun testGetScreenName() {
        val expectedValue = "myScreen"
        NeuroIDImpl.screenName = expectedValue

        val value = NeuroIDImpl.getInstance()?.getScreenName()

        assertEquals(expectedValue, value)
    }

    //    excludeViewByTestID
    @Test
    fun testExcludeViewByTestID_single() {
        NeuroIDImpl.getInstance()?.excludeViewByTestID("excludeMe")

        assertEquals(1, NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.count())
        assertEquals("excludeMe", NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.first())
    }

    @Test
    fun testExcludeViewByTestID_double() {
        NeuroIDImpl.getInstance()?.excludeViewByTestID("excludeMe")
        assertEquals(1, NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.count())
        assertEquals("excludeMe", NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.first())

        NeuroIDImpl.getInstance()?.excludeViewByTestID("excludeMe")
        assertEquals(1, NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.count())
    }

    //    setEnvironment - DEPRECATED
    @Test
    fun testSetEnvironment() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironment"))

        NeuroIDImpl.environment = ""

        NeuroIDImpl.getInstance()?.setEnvironment("MYENV")

        assertEquals("", NeuroIDImpl.environment)
        assertInfoCount(1)
    }

    //    setEnvironmentProduction - DEPRECATED
    @Test
    fun testSetEnvironmentProduction_true() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NeuroIDImpl.environment = ""

        NeuroIDImpl.getInstance()?.setEnvironmentProduction(true)

        assertEquals("", NeuroIDImpl.environment)
        assertInfoCount(1)
    }

    @Test
    fun testSetEnvironmentProduction_false() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setEnvironmentProduction"))

        NeuroIDImpl.environment = ""

        NeuroIDImpl.getInstance()?.setEnvironmentProduction(false)

        assertEquals("", NeuroIDImpl.environment)
        assertInfoCount(1)
    }

    //    getEnvironment - DEPRECATED
    @Test
    fun testGetEnvironment() {

        val expectedValue = "MyEnv"
        NeuroIDImpl.environment = expectedValue


        val value = NeuroIDImpl.getInstance()?.getEnvironment()

        assertEquals(expectedValue, value)
    }

    //    setSiteId - DEPRECATED
    @Test
    fun testSetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("setSiteId"))

        val expectedValue = "TestSiteId"
        NeuroIDImpl.siteID = "DifferentSiteID"

        NeuroIDImpl.getInstance()?.setSiteId(expectedValue)

        assertEquals(expectedValue, NeuroIDImpl.siteID)
        assertInfoCount(1)
    }

    //    getSiteId - DEPRECATED
    @Test
    fun testGetSiteId() {
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("getSiteId"))

        val expectedValue = ""
        NeuroIDImpl.siteID = "TestSiteId"

        val value = NeuroIDImpl.getInternalInstance()?.getSiteId()

        assertEquals(expectedValue, value)
        assertInfoCount(1)
    }

    //    getSessionId
    @Test
    fun testGetSessionID() {

        val expectedValue = "testSessionID"
        NeuroIDImpl.getInternalInstance()?.sessionID = expectedValue

        val value = NeuroIDImpl.getInternalInstance()?.getSessionID()

        assertEquals(expectedValue, value)
    }

    //    getClientId
    @Test
    fun testGetClientID() {

        val expectedValue = "testClientID"
        NeuroIDImpl.getInternalInstance()?.clientID = expectedValue

        val value = NeuroIDImpl.getInstance()?.getClientID()

        assertEquals(expectedValue, value)
    }

    //    shouldForceStart
    @Test
    fun test_shouldForceStart_false() {

        val expectedValue = false

        val value = NeuroIDImpl.getInternalInstance()?.shouldForceStart()

        assertEquals(expectedValue, value)
    }

    @Test
    fun test_shouldForceStart_true() {

        val expectedValue = true
        NeuroIDImpl.getInternalInstance()?.forceStart = expectedValue

        val value = NeuroIDImpl.getInternalInstance()?.shouldForceStart()

        assertEquals(expectedValue, value)
    }

    //    registerPageTargets
    @Test
    fun testRegisterPageTargets() {
        val mockedNIDACB = mockk<ActivityCallbacks>()
        every { mockedNIDACB.forceStart(any()) } just runs
        NeuroIDImpl.getInternalInstance()?.setNIDActivityCallbackInstance(mockedNIDACB)

        val mockedActivity = mockk<Activity>()

        NeuroIDImpl.getInstance()?.registerPageTargets(mockedActivity)

        assertEquals(true, NeuroIDImpl.getInternalInstance()?.forceStart)
        verify { mockedNIDACB.forceStart(mockedActivity) }

        // reset for other tests
        NeuroIDImpl.getInternalInstance()?.forceStart = false
    }

    //    getTabId
    @Test
    fun testGetTabId() {
        val expectedValue = "MyRNDID"

        NeuroIDImpl.rndmId = expectedValue

        val value = NeuroIDImpl.getInternalInstance()?.getTabId()

        assertEquals(expectedValue, value)
    }

    //    getFirstTS - not worth testing
    @Test
    fun testGetFirstTS() {
        val expectedValue: Long = 1234

        NeuroIDImpl.getInternalInstance()?.timestamp = expectedValue

        val value = NeuroIDImpl.getInternalInstance()?.getFirstTS()

        assertEquals(expectedValue, value)
    }

    //    formSubmit - Deprecated
    @Test
    fun testFormSubmit() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmit"))

        NeuroIDImpl.getInstance()?.formSubmit()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === APPLICATION_SUBMIT)
    }

    //    formSubmitSuccess - Deprecated
    @Test
    fun testFormSubmitSuccess() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmitSuccess"))

        NeuroIDImpl.getInstance()?.formSubmitSuccess()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === FORM_SUBMIT_SUCCESS)
    }

    //    formSubmitFailure - Deprecated
    @Test
    fun testFormSubmitFailure() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmitFailure"))

        NeuroIDImpl.getInstance()?.formSubmitFailure()
        assertInfoCount(1)

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === FORM_SUBMIT_FAILURE)
    }

    //    start
    @Test
    fun testStart_success() {
        setMockedNIDJobServiceManager()

        NeuroIDImpl.isSDKStarted = false
        NeuroIDImpl.getInternalInstance()?.clientKey = "abcd"
        val value = NeuroIDImpl.getInstance()?.start()

        assertEquals(true, value)
        assertEquals(true, NeuroIDImpl.isSDKStarted)
    }

    @Test
    fun testStart_failure() {
        setMockedNIDJobServiceManager()

        setNeuroIDMockedLogger(
            errorMessage = "Missing Client Key - please call configure prior to calling start"
        )

        NeuroIDImpl.isSDKStarted = false

        NeuroIDImpl.getInternalInstance()?.clientKey = ""

        val value = NeuroIDImpl.getInstance()?.start()

        assertEquals("", NeuroIDImpl.getInternalInstance()?.clientKey)
        assertEquals(false, value)
        assertEquals(false, NeuroIDImpl.isSDKStarted)

        assertErrorCount(1)
    }

    //    stop
    @Test
    fun testStop() {
        setMockedNIDJobServiceManager()

        NeuroIDImpl.isSDKStarted = true

        NeuroIDImpl.getInstance()?.stop()

        assertEquals(false, NeuroIDImpl.isSDKStarted)
    }

//    closeSession - Need to mock NIDJobServiceManager
//    resetClientId - Need to mock Application & Shared Preferences

    //    isStopped
    @Test
    fun testIsStopped_true() {
        setMockedNIDJobServiceManager()

        val expectedValue = true
        NeuroIDImpl.isSDKStarted = !expectedValue

        val value = NeuroIDImpl.getInstance()?.isStopped()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testIsStopped_false() {
        setMockedNIDJobServiceManager()

        val expectedValue = false
        NeuroIDImpl.isSDKStarted = !expectedValue

        val value = NeuroIDImpl.getInstance()?.isStopped()

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
        NeuroIDImpl.getInternalInstance()?.application = mockedApplication


        var value = NeuroIDImpl.getInternalInstance()?.getApplicationContext()
        assertEquals(mockedContext, value)
    }

//    createSession - Need to mock Application
//    createMobileMetadata - Need to mock Application

    // setIsRN
    @Test
    fun testSetIsRN() {
        NeuroIDImpl.getInternalInstance()?.isRN = false
        assertEquals(false, NeuroIDImpl.getInternalInstance()?.isRN)

        NeuroIDImpl.getInstance()?.setIsRN()

        assertEquals(true, NeuroIDImpl.getInternalInstance()?.isRN)
    }

    //    enableLogging
    @Test
    fun testEnableLogging_true() {
        NeuroIDImpl.showLogs = false

        NeuroIDImpl.getInstance()?.enableLogging(true)

        assertEquals(true, NeuroIDImpl.showLogs)
    }

    @Test
    fun testEnableLogging_false() {
        NeuroIDImpl.showLogs = true

        NeuroIDImpl.getInstance()?.enableLogging(false)

        assertEquals(false, NeuroIDImpl.showLogs)
    }

    //    getSDKVersion
    @Test
    fun testGetSDKVersion_notRN() {
        NeuroIDImpl.getInternalInstance()?.isRN = false

        val version = NeuroIDImpl.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-"))
        assertEquals(false, version?.contains("rn-"))
    }

    @Test
    fun testGetSDKVersion_RN() {
        NeuroIDImpl.getInternalInstance()?.isRN = true

        val version = NeuroIDImpl.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-rn"))
    }

    //    clearSessionVariables
    @Test
    fun testClearSessionVariables() {
        NeuroIDImpl.getInternalInstance()?.userID = "myID"
        NeuroIDImpl.getInternalInstance()?.registeredUserID = "myRID"

        NeuroIDImpl.getInstance()?.clearSessionVariables()

        assertEquals("", NeuroIDImpl.getInternalInstance()?.userID)
        assertEquals("", NeuroIDImpl.getInternalInstance()?.registeredUserID)
    }

    //    startSession
    @Test
    fun testStartSession_success_no_id() {
        setMockedNIDJobServiceManager()
        setMockedDataStore()
        NeuroIDImpl.getInternalInstance()?.let {
            it.clientKey = "dummyKey"
            val (started, id) = it.startSession()
            assertEquals(true, started)
            assertNotEquals("", id)
        }
    }

    @Test
    fun testStartSession_success_id() {
        setMockedNIDJobServiceManager()
        setMockedDataStore()
        NeuroIDImpl.getInternalInstance()?.let {
            it.clientKey = "dummyKey"
            val (started, id) = it.startSession("testID")
            assertEquals(true, started)
            assertEquals("testID", id)
        }
    }

    @Test
    fun testStartSession_failure_clientKey() {
        setMockedNIDJobServiceManager()
        setNeuroIDMockedLogger(
            errorMessage = "Missing Client Key - please call configure prior to calling start"
        )
        NeuroIDImpl.getInternalInstance()?.let {
            it.clientKey = ""
            val (started, id) = it.startSession()
            assertEquals(false, started)
            assertEquals("", id)

            assertErrorCount(1)
        }
    }

    @Test
    fun testStartSession_failure_userID() {
        setMockedNIDJobServiceManager()
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")
        NeuroIDImpl.getInternalInstance()?.let {
            it.clientKey = "dummyKey"
            val result = it.startSession("bad user 343%%^")
            assertEquals(false, result.started)
            assertEquals("", result.sessionID)

            assertErrorCount(1)
        }
    }

    //    pauseCollection
    @Test
    fun testPauseCollection() {
        setMockedNIDJobServiceManager()
        NeuroIDImpl.getInstance()?.let {
            it.pauseCollection()
            assertEquals(false, NeuroIDImpl.isSDKStarted)
        }
    }

    //    resumeCollection
    @Test
    fun testResumeCollection() {
        setMockedNIDJobServiceManager()
        NeuroIDImpl.getInstance()?.pauseCollection()
        NeuroIDImpl.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(true, NeuroIDImpl.isSDKStarted)
                }
            } else {
                assertEquals(true, NeuroIDImpl.isSDKStarted)
            }

        }
    }

    @Test
    fun testResumeCollection_SDK_is_stopped_no_userId() {
        setMockedNIDJobServiceManager()
        NeuroIDImpl.getInstance()?.stopSession()
        NeuroIDImpl.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(false, NeuroIDImpl.isSDKStarted)
                }
            } else {
                assertEquals(false, NeuroIDImpl.isSDKStarted)
            }

        }
    }

    @Test
    fun testResumeCollection_SDK_is_stopped_userId() {
        setMockedNIDJobServiceManager()
        NeuroIDImpl.getInstance()?.stopSession()
        NeuroIDImpl.getInstance()?.setUserID("gasdgasdgasd")
        NeuroIDImpl.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(true, NeuroIDImpl.isSDKStarted)
                }
            } else {
                assertEquals(true, NeuroIDImpl.isSDKStarted)
            }

        }
    }

    //    stopSession
    @Test
    fun testStopSession() {
        setMockedNIDJobServiceManager()
        NeuroIDImpl.getInstance()?.let {
            val stopped = it.stopSession()
            assertEquals(true, stopped)
            assertEquals(false, NeuroIDImpl.isSDKStarted)
        }
    }

    @Test
    fun testGetOriginResult_CUSTOMER_SET_OK() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInternalInstance()?.let {
            val sessionID = "gasdgasdgdsgds"
            val result = it.getOriginResult(sessionID, true, true)
            assertEquals(result.origin, NID_ORIGIN_CUSTOMER_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_CUSTOMER)
            assertEquals(result.sessionID, sessionID)
        }
    }

    @Test
    fun testGetOriginResult_CUSTOMER_SET_FAIL() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInternalInstance()?.let {
            val badSessionID = "gasdgas dgdsgds"
            val result = it.getOriginResult(badSessionID, false, true)
            assertEquals(result.origin, NID_ORIGIN_CUSTOMER_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
        }
    }

    @Test
    fun testGetOriginResult_CUSTOMER_SET_EMPTY_SESSION_ID() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInternalInstance()?.let {
            val emptySessionID = ""
            val result = it.getOriginResult(emptySessionID, true, false)
            assertEquals(result.origin, NID_ORIGIN_NID_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_NID)
        }
    }

    @Test
    fun testGetOriginResult_NID_SET_EMPTY_SESSION_ID() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInternalInstance()?.let {
            val emptySessionID = ""
            val result = it.getOriginResult(emptySessionID, false, false)
            assertEquals(result.origin, NID_ORIGIN_NID_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
        }
    }

    @Test
    fun testSetRegisteredUserId_not_empty() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInstance()?.let {
            val result = it.setRegisteredUserID("gdsgdsgsd")
            assertTrue(result)
            assertEquals(it.getRegisteredUserID(), "gdsgdsgsd")
        }
    }

    @Test
    fun testSetRegisteredUserId_empty() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInstance()?.let {
            it.setRegisteredUserID("gdsgdsgsd")
            val result = it.setRegisteredUserID("")
            assertFalse(result)
            assertEquals("gdsgdsgsd", it.getRegisteredUserID())
        }
    }

    @Test
    fun testSetUserId_not_empty() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInstance()?.let {
            val result = it.setUserID("gdsgdsgsdzzzz")
            assertTrue(result)
            assertEquals(it.getUserID(), "gdsgdsgsdzzzz")
        }
    }

    @Test
    fun testSetUserId_empty() {
        unsetDefaultMockedLogger()
        NeuroIDImpl.getInstance()?.let {
            it.setUserID("gdsgdsgsdzzzz")
            val result = it.setUserID("")
            assertFalse(result)
            assertEquals("gdsgdsgsdzzzz", it.getUserID())
        }
    }


    //    captureEvent
    @Test
    fun testCaptureEvent_success() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_success_queued() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.captureEvent(queuedEvent = true, type= "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(1, queuedEvents.count())
        assertEquals(true, queuedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_failure_not_started() {
        NeuroIDImpl.isSDKStarted = false
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_jobManager_stopped() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(true)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tgs() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent", tgs = "excludeMe")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tg() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroIDImpl.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent", tg = mapOf(
            "tgs" to "excludeMe"
        ))

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_lowMemory() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroIDImpl.getInternalInstance()?.lowMemory = true

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())

        assertWarningCount(1)

        NeuroIDImpl.getInternalInstance()?.lowMemory = false
    }

    @Test
    fun testCaptureEvent_failure_fullBuffer() {
        NeuroIDImpl.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore(true)
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroIDImpl.getInternalInstance()?.captureEvent(type= "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(0, storedEvents.count())

        assertWarningCount(1)
    }
}