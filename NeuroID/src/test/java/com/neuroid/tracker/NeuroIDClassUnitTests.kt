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
import java.util.Calendar


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
        NeuroID.Builder(null, "key_test_fake1234", NeuroID.DEVELOPMENT).build()
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

        NeuroID.getInternalInstance()?.setLoggerInstance(log)
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

        NeuroID.getInternalInstance()?.setDataStoreInstance(dataStoreManager)

        return dataStoreManager
    }

    private fun setMockedApplication() {
        val mockedApplication = mockk<Application>()

        every { mockedApplication.applicationContext } answers {
            mockk<Context>()
        }

        NeuroID.getInternalInstance()?.application = mockedApplication
    }

    private fun setMockedNIDJobServiceManager(isStopped:Boolean = true) {
        val mockedNIDJobServiceManager = mockk<NIDJobServiceManager>()

        every { mockedNIDJobServiceManager.startJob(any(), any()) } just runs
        every { mockedNIDJobServiceManager.isStopped() } returns isStopped
        every { mockedNIDJobServiceManager.stopJob() } just runs

        coEvery {
            mockedNIDJobServiceManager.sendEvents(any())
        } just runs

        NeuroID.getInternalInstance()?.setNIDJobServiceManager(mockedNIDJobServiceManager)
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
        unmockkAll()
    }

    // Function Tests

    //    setLoggerInstance - Used for mocking
    //    setDataStoreInstance - Used for mocking
    //    setNIDActivityCallbackInstance - Used for mocking
    //    setNIDJobServiceManager - Used for mocking

    //   setTestURL

    private fun setupAttemptedLoginTestEnvironment() {
        // fake out the clock
        mockkStatic(Calendar::class)
        every {Calendar.getInstance().timeInMillis} returns 1
        // make the logger not throw
        val logger = mockk<NIDLogWrapper>()
        every { logger.e(any(), any()) } just runs
        NeuroID.getInternalInstance()?.logger = logger
        // everything else as normal
        setMockedDataStore()
        setMockedNIDJobServiceManager(false)
        NeuroID.isSDKStarted = true
    }

    private fun testAttemptedLogin(userId: String?, expectedHash: String, expectedResult: Boolean) {
        setupAttemptedLoginTestEnvironment()
        val dataStoreManager = NeuroID.getInternalInstance()?.dataStore
        val actualResult = NeuroID.getInstance()?.attemptedLogin(userId)
        verify {dataStoreManager?.saveEvent(NIDEventModel(ts=1, type="ATTEMPTED_LOGIN", uid="$expectedHash"))}
        assertEquals(expectedResult, actualResult)
        unmockkStatic(Calendar::class)
    }

    @Test
    fun testAttemptedLoginVarious() {
        setupAttemptedLoginTestEnvironment()
        // the single good id
        testAttemptedLogin("goodone", "207034505", true)
        // all the rest are rubbish ids
        testAttemptedLogin("12", "scrubbed-id-failed-validation", false)
        testAttemptedLogin("test@test.com'", "scrubbed-id-failed-validation", false)
        testAttemptedLogin(null, "scrubbed-id-failed-validation", false)
        testAttemptedLogin("@#\$%^&*()", "scrubbed-id-failed-validation" , false)
        testAttemptedLogin("¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂ", "scrubbed-id-failed-validation" , false)
        testAttemptedLogin("ÃÄÅÆÇÈÉ ÊË Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö", "scrubbed-id-failed-validation" , false)
        testAttemptedLogin("almost good", "scrubbed-id-failed-validation", false);
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

    //    validateClientKey
    @Test
    fun testValidateClientKey_bad_key() {
        val value = NeuroID.getInternalInstance()?.validateClientKey("kjjhgh")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_invalid_key() {
        val value = NeuroID.getInternalInstance()?.validateClientKey("key_tert_fdffsd")

        assertEquals(false, value)
    }

    @Test
    fun testValidateClientKey_valid_key() {
        val value = NeuroID.getInternalInstance()?.validateClientKey("key_test_1235")

        assertEquals(true, value)
    }

    //    validateUserId
    @Test
    fun testValidateUserID_valid() {
        val value = NeuroID.getInternalInstance()?.validateUserId("goodUserId")

        assertEquals(true, value)
    }

    @Test
    fun testValidateUserID_invalid() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")
        val value = NeuroID.getInternalInstance()?.validateUserId("bad userID")

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
        assertEquals(4, queuedEvents.count())
        assertEquals(1, queuedEvents.count{it.type === SET_USER_ID})
        assertEquals(3, queuedEvents.count { it.type === SET_VARIABLE  })
    }

    @Test
    fun testSetRegisteredUserID_success_notStarted() {
        setMockedDataStore()
        NeuroID.isSDKStarted = false

        val value = NeuroID.getInstance()?.setRegisteredUserID("myUserID")

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

        NeuroID.isSDKStarted = true

        val value = NeuroID.getInstance()?.setUserID("myUserID")

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

        NeuroID.isSDKStarted = true

        val value = NeuroID.getInstance()?.setRegisteredUserID("myRegisteredUserID")

        assertEquals(true, value)
        assertEquals(4, storedEvents.count())
        assertEquals(3, storedEvents.count { x -> x.type == SET_VARIABLE })
        assertEquals(1, storedEvents.count { x -> x.type === SET_REGISTERED_USER_ID })
    }

    @Test
    fun testSetUserID_failure() {
        setMockedDataStore(false)
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        val value = NeuroID.getInstance()?.setUserID("Bad UserID")

        assertEquals(false, value)
        assertErrorCount(1)
    }

    @Test
    fun testSetRegisteredUserID_failure() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        val value = NeuroID.getInstance()?.setRegisteredUserID("Bad User REGISTERED ID")

        assertEquals(false, value)
        assertErrorCount(1)
        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testSetRegisteredUserID_failure_Started() {
        setNeuroIDMockedLogger(errorMessage = "Invalid UserID")

        NeuroID.isSDKStarted = true

        val value = NeuroID.getInstance()?.setRegisteredUserID("Bad User REGISTERED ID")

        assertEquals(false, value)
        assertErrorCount(1)
        assertEquals(0, storedEvents.count())
    }

    //    getUserId
    @Test
    fun testGetUserID() {
        val expectedValue = "testID"
        NeuroID.getInternalInstance()?.userID = expectedValue
        val value = NeuroID.getInstance()?.getUserID()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testGetRegisteredUserID() {
        val expectedValue = "testRegisteredID"
        NeuroID.getInternalInstance()?.registeredUserID = expectedValue
        val value = NeuroID.getInstance()?.getRegisteredUserID()

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

        NeuroID.rndmId = expectedValue

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
        NeuroID.isSDKStarted = true
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
        NeuroID.isSDKStarted = true
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
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
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
        setMockedNIDJobServiceManager()

        NeuroID.isSDKStarted = false
        NeuroID.getInternalInstance()?.clientKey = "abcd"
        val value = NeuroID.getInstance()?.start()

        assertEquals(true, value)
        assertEquals(true, NeuroID.isSDKStarted)
    }

    @Test
    fun testStart_failure() {
        setMockedNIDJobServiceManager()

        setNeuroIDMockedLogger(
            errorMessage = "Missing Client Key - please call configure prior to calling start"
        )

        NeuroID.isSDKStarted = false

        NeuroID.getInternalInstance()?.clientKey = ""

        val value = NeuroID.getInstance()?.start()

        assertEquals("", NeuroID.getInternalInstance()?.clientKey)
        assertEquals(false, value)
        assertEquals(false, NeuroID.isSDKStarted)

        assertErrorCount(1)
    }

    //    stop
    @Test
    fun testStop() {
        setMockedNIDJobServiceManager()

        NeuroID.isSDKStarted = true

        NeuroID.getInstance()?.stop()

        assertEquals(false, NeuroID.isSDKStarted)
    }

//    closeSession - Need to mock NIDJobServiceManager
//    resetClientId - Need to mock Application & Shared Preferences

    //    isStopped
    @Test
    fun testIsStopped_true() {
        setMockedNIDJobServiceManager()

        val expectedValue = true
        NeuroID.isSDKStarted = !expectedValue

        val value = NeuroID.getInstance()?.isStopped()

        assertEquals(expectedValue, value)
    }

    @Test
    fun testIsStopped_false() {
        setMockedNIDJobServiceManager()

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

    //    getSDKVersion
    @Test
    fun testGetSDKVersion_notRN() {
        NeuroID.getInternalInstance()?.isRN = false

        val version = NeuroID.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-"))
        assertEquals(false, version?.contains("rn-"))
    }

    @Test
    fun testGetSDKVersion_RN() {
        NeuroID.getInternalInstance()?.isRN = true

        val version = NeuroID.getInstance()?.getSDKVersion()

        assertEquals(true, version?.contains("5.android-rn"))
    }

    //    clearSessionVariables
    @Test
    fun testClearSessionVariables() {
        NeuroID.getInternalInstance()?.userID = "myID"
        NeuroID.getInternalInstance()?.registeredUserID = "myRID"

        NeuroID.getInternalInstance()?.clearSessionVariables()

        assertEquals("", NeuroID.getInternalInstance()?.userID)
        assertEquals("", NeuroID.getInternalInstance()?.registeredUserID)
    }

    //    startSession
    @Test
    fun testStartSession_success_no_id() {
        setMockedNIDJobServiceManager()
        setMockedDataStore()
        NeuroID.getInternalInstance()?.let {
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
        NeuroID.getInternalInstance()?.let {
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
        NeuroID.getInternalInstance()?.let {
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
        NeuroID.getInternalInstance()?.let {
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
        NeuroID.getInstance()?.let {
            it.pauseCollection()
            assertEquals(false, NeuroID.isSDKStarted)
        }
    }

    //    resumeCollection
    @Test
    fun testResumeCollection() {
        setMockedNIDJobServiceManager()
        NeuroID.getInstance()?.pauseCollection()
        NeuroID.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(true, NeuroID.isSDKStarted)
                }
            } else {
                assertEquals(true, NeuroID.isSDKStarted)
            }

        }
    }

    @Test
    fun testResumeCollection_SDK_is_stopped_no_userId() {
        setMockedNIDJobServiceManager()
        NeuroID.getInstance()?.stopSession()
        NeuroID.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(false, NeuroID.isSDKStarted)
                }
            } else {
                assertEquals(false, NeuroID.isSDKStarted)
            }

        }
    }

    @Test
    fun testResumeCollection_SDK_is_stopped_userId() {
        setMockedNIDJobServiceManager()
        NeuroID.getInstance()?.stopSession()
        NeuroID.getInstance()?.setUserID("gasdgasdgasd")
        NeuroID.getInternalInstance()?.let {
            it.resumeCollection()

            if (it.pauseCollectionJob != null) {
                it.pauseCollectionJob?.invokeOnCompletion {
                    assertEquals(true, NeuroID.isSDKStarted)
                }
            } else {
                assertEquals(true, NeuroID.isSDKStarted)
            }

        }
    }

    //    stopSession
    @Test
    fun testStopSession() {
        setMockedNIDJobServiceManager()
        NeuroID.getInstance()?.let {
            val stopped = it.stopSession()
            assertEquals(true, stopped)
            assertEquals(false, NeuroID.isSDKStarted)
        }
    }

    @Test
    fun testGetOriginResult_CUSTOMER_SET_OK() {
        unsetDefaultMockedLogger()
        NeuroID.getInternalInstance()?.let {
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
        NeuroID.getInternalInstance()?.let {
            val badSessionID = "gasdgas dgdsgds"
            val result = it.getOriginResult(badSessionID, false, true)
            assertEquals(result.origin, NID_ORIGIN_CUSTOMER_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
        }
    }

    @Test
    fun testGetOriginResult_CUSTOMER_SET_EMPTY_SESSION_ID() {
        unsetDefaultMockedLogger()
        NeuroID.getInternalInstance()?.let {
            val emptySessionID = ""
            val result = it.getOriginResult(emptySessionID, true, false)
            assertEquals(result.origin, NID_ORIGIN_NID_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_NID)
        }
    }

    @Test
    fun testGetOriginResult_NID_SET_EMPTY_SESSION_ID() {
        unsetDefaultMockedLogger()
        NeuroID.getInternalInstance()?.let {
            val emptySessionID = ""
            val result = it.getOriginResult(emptySessionID, false, false)
            assertEquals(result.origin, NID_ORIGIN_NID_SET)
            assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
        }
    }

    @Test
    fun testSetRegisteredUserId_not_empty() {
        unsetDefaultMockedLogger()
        NeuroID.getInstance()?.let {
            val result = it.setRegisteredUserID("gdsgdsgsd")
            assertTrue(result)
            assertEquals(it.getRegisteredUserID(), "gdsgdsgsd")
        }
    }

    @Test
    fun testSetRegisteredUserId_empty() {
        unsetDefaultMockedLogger()
        NeuroID.getInstance()?.let {
            it.setRegisteredUserID("gdsgdsgsd")
            val result = it.setRegisteredUserID("")
            assertFalse(result)
            assertEquals("gdsgdsgsd", it.getRegisteredUserID())
        }
    }

    @Test
    fun testSetUserId_not_empty() {
        unsetDefaultMockedLogger()
        NeuroID.getInstance()?.let {
            val result = it.setUserID("gdsgdsgsdzzzz")
            assertTrue(result)
            assertEquals(it.getUserID(), "gdsgdsgsdzzzz")
        }
    }

    @Test
    fun testSetUserId_empty() {
        unsetDefaultMockedLogger()
        NeuroID.getInstance()?.let {
            it.setUserID("gdsgdsgsdzzzz")
            val result = it.setUserID("")
            assertFalse(result)
            assertEquals("gdsgdsgsdzzzz", it.getUserID())
        }
    }


    //    captureEvent
    @Test
    fun testCaptureEvent_success() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(1, storedEvents.count())
        assertEquals(true, storedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_success_queued() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(queuedEvent = true, type= "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(1, queuedEvents.count())
        assertEquals(true, queuedEvents.firstOrNull()?.type === "testEvent")
    }

    @Test
    fun testCaptureEvent_failure_not_started() {
        NeuroID.isSDKStarted = false
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_jobManager_stopped() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(true)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tgs() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent", tgs = "excludeMe")

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_excludedID_tg() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()

        NeuroID.getInternalInstance()?.excludedTestIDList?.add("excludeMe")

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent", tg = mapOf(
            "tgs" to "excludeMe"
        ))

        assertEquals(0, storedEvents.count())
    }

    @Test
    fun testCaptureEvent_failure_lowMemory() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore()
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroID.getInternalInstance()?.lowMemory = true

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent")

        assertEquals(0, storedEvents.count())

        assertWarningCount(1)

        NeuroID.getInternalInstance()?.lowMemory = false
    }

    @Test
    fun testCaptureEvent_failure_fullBuffer() {
        NeuroID.isSDKStarted = true
        setMockedNIDJobServiceManager(false)
        setMockedDataStore(true)
        setMockedApplication()
        setNeuroIDMockedLogger(warningMessage = "Data store buffer FULL_BUFFER, testEvent dropped")

        NeuroID.getInternalInstance()?.captureEvent(type= "testEvent")

        // In reality the datastore would add a full buffer event but that is tested in
        //    the datastore unit tests
        assertEquals(0, storedEvents.count())

        assertWarningCount(1)
    }
}