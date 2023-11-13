package com.neuroid.tracker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test


class NeuroIDClassUnitTests {
    // Helper Functions
    private fun setNeuroIDInstance() {
        val neuroId = NeuroID.Builder(
            null,
            "key_test_fake1234"
        ).build()
        NeuroID.setNeuroIdInstance(neuroId)
    }

    private fun setNeuroIDMockedLogger() {
        val log = mockk<NIDLogWrapper>()

        justRun { log.e(any(), any()) }
        justRun { log.i(any(), any()) }
        justRun { log.d(any(), any()) }

        NeuroID.getInstance()?.setLoggerInstance(log)
    }


    @Before
    fun setUp() {
        // setup instance and logging
        setNeuroIDInstance()
        setNeuroIDMockedLogger()

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

        val value = NeuroID.getInstance()?.validateUserId("bad userID")

        assertEquals(false, value)
    }

    //    setUserID
    @Test
    fun testSetUserID_success() {

        val value = NeuroID.getInstance()?.setUserID("myUserID")

        assertEquals(true, value)
    }

    @Test
    fun testSetUserID_failure() {

        val value = NeuroID.getInstance()?.setUserID("Bad UserID")

        assertEquals(false, value)
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

        NeuroID.isSDKStarted = false

        val value = NeuroID.getInstance()?.setScreenName("testName")

        assertEquals(false, value)
    }

    //    excludeViewByResourceID - Need to mock

    //    setEnvironment - DEPRECATED
    @Test
    fun testSetEnvironment() {

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironment("MYENV")

        assertEquals("", NIDServiceTracker.environment)
    }

    //    setEnvironmentProduction - DEPRECATED
    @Test
    fun testSetEnvironmentProduction_true() {

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(true)

        assertEquals("", NIDServiceTracker.environment)
    }

    @Test
    fun testSetEnvironmentProduction_false() {

        NIDServiceTracker.environment = ""

        NeuroID.getInstance()?.setEnvironmentProduction(false)

        assertEquals("", NIDServiceTracker.environment)
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

        val expectedValue = "TestSiteId"
        NIDServiceTracker.siteId = "DifferentSiteID"

        NeuroID.getInstance()?.setSiteId(expectedValue)

        assertEquals(expectedValue, NIDServiceTracker.siteId)
    }

    //    getSiteId - DEPRECATED
    @Test
    fun testGetSiteId() {

        val expectedValue = ""
        NIDServiceTracker.siteId = "TestSiteId"

        val value = NeuroID.getInstance()?.getSiteId()

        assertEquals(expectedValue, value)
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
    fun testGetForceStart_null() {

        val expectedValue = null

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


    //    setForceStart - unsure how to mock

    //    getTabId
    @Test
    fun testGetTabId() {
        val expectedValue = "MyRNDID"

        NIDServiceTracker.rndmId = expectedValue

        val value = NeuroID.getInstance()?.getTabId()

        assertEquals(expectedValue, value)
    }

//    getFirstTS - not woth testing

//    getJsonPayLoad - not sure how to mock
//    resetJsonPayLoad - not sure how to mock

    //    captureEvent - not sure how to mock Application context
    @Test
    @Ignore
    fun testCaptureEvent() = runTest {

        getDataStoreInstance().clearEvents()
        advanceUntilIdle()
        var events = getDataStoreInstance().getAllEvents()
        assertEquals(0, events.count())
        advanceUntilIdle()

        NeuroID.getInstance()?.captureEvent("test", "tgs")

        events = getDataStoreInstance().getAllEvents()
        println("EVENTA: ${events.toString()} - ${events.count()} - ${events}")
        advanceUntilIdle()
        assertEquals(1, events.count())
    }

    //    formSubmit
    @Test
    fun testFormSubmit() = runTest {

        getDataStoreInstance().clearEvents()
        advanceUntilIdle()

        var events = getDataStoreInstance().getAllEvents()
        assertEquals(0, events.count())
        advanceUntilIdle()


//        NeuroID.getInstance()?.formSubmit()
        getDataStoreInstance().saveEvent(NIDEventModel(type = "dfdfd", ts = 123))
        advanceUntilIdle()
        
        events = getDataStoreInstance().getAllEvents()
        println("EVENTA: ${events.toString()} - ${events.count()} - ${events}")
        advanceUntilIdle()
        assertEquals(1, events.count())
    }

//    formSubmitSuccess
//    formSubmitFailure
//    configureWithOptions
//    start
//    stop
//    closeSession
//    resetClientId
//    isStopped
//    registerTarget
//    getApplicationContext
//    createSession
//    createMobileMetadata
//    setIsRN
//    enableLogging


    @Test
    fun testStart_success() {
        setNeuroIDMockedLogger()

        val value = NeuroID.getInstance()?.start()

        assertEquals(true, value)
        assertEquals(true, NeuroID.isSDKStarted)
    }

    @Test
    fun testStart_failure() {
        setNeuroIDMockedLogger()
        NeuroID.isSDKStarted = false

        NeuroID.getInstance()?.clientKey = ""

        val value = NeuroID.getInstance()?.start()

        assertEquals("", NeuroID.getInstance()?.clientKey)
        assertEquals(false, value)
        assertEquals(false, NeuroID.isSDKStarted)
    }
}