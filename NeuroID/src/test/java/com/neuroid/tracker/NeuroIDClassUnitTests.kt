package com.neuroid.tracker

import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.utils.NIDLogWrapper

import io.mockk.every
import io.mockk.mockk

import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.assertEquals

// uncomment once using datastore
//import kotlinx.coroutines.test.advanceUntilIdle
//import kotlinx.coroutines.test.runTest


enum class TestLogLevel {
    DEBUG,
    INFO,
    ERROR
}

class NeuroIDClassUnitTests {
    private var errorCount = 0
    private var infoCount = 0
    private var debugCount = 0

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
        setNeuroIDMockedLogger()

        clearLogCounts()
    }

    @After
    fun tearDown() {
        assertEquals("Expected Log Error Count is Greater than 0", 0, errorCount)
        assertEquals("Expected Log Info Count is Greater than 0", 0, infoCount)
        assertEquals("Expected Log Debug Count is Greater than 0", 0, debugCount)
    }

    // function tests

    //    setLoggerInstance - Can't mock?

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
    fun testSetUserID_success() {

        val value = NeuroID.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
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
        val value = NeuroID.getInstance()?.getUserId()

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

    //    excludeViewByResourceID - Need to mock

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
    fun testGetSessionId() {

        val expectedValue = "testSessionID"
        NeuroID.getInstance()?.sessionID = expectedValue

        val value = NeuroID.getInstance()?.getSessionId()

        assertEquals(expectedValue, value)
    }

    //    getClientId
    @Test
    fun testGetClientId() {

        val expectedValue = "testClientID"
        NeuroID.getInstance()?.clientID = expectedValue

        val value = NeuroID.getInstance()?.getClientId()

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


    //    registerPageTargets - unsure how to mock

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
    @Ignore("Ignore until mocked Application")
    fun testCaptureEvent() {
//        = runTest

//        getDataStoreInstance().clearEvents()
//        advanceUntilIdle()
//        var events = getDataStoreInstance().getAllEvents()
//        assertEquals(0, events.count())
//        advanceUntilIdle()
//
//        NeuroID.getInstance()?.captureEvent("test", "tgs")
//
//        events = getDataStoreInstance().getAllEvents()
//        println("EVENTA: ${events.toString()} - ${events.count()} - ${events}")
//        advanceUntilIdle()
//        assertEquals(1, events.count())
    }

    //    formSubmit - Need to mock Datastore
    @Test
    @Ignore("Ignore until mocked Datastore")
    fun testFormSubmit() {
//        = runTest {  }
//        setNeuroIDMockedLogger(infoMessage = getDeprecatedMessage("formSubmit"))

//        NeuroID.getInstance()?.formSubmit()
//        assertInfoCount(1)
    }

//    formSubmitSuccess - Need to mock Datastore
//    formSubmitFailure - Need to mock Datastore

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
//    resetClientId - Need to mock Application
//    isStopped - Need to mock NIDJobServiceManager - Should change to isSDKStarted variable
//    registerTarget - Need to mock Activity
//    getApplicationContext - Need to mock Application
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
}