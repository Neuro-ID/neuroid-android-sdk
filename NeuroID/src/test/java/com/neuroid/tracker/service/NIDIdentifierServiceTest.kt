package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.NID_ORIGIN_CODE_CUSTOMER
import com.neuroid.tracker.events.NID_ORIGIN_CODE_FAIL
import com.neuroid.tracker.events.NID_ORIGIN_CODE_NID
import com.neuroid.tracker.events.NID_ORIGIN_CUSTOMER_SET
import com.neuroid.tracker.events.NID_ORIGIN_NID_SET
import com.neuroid.tracker.events.SET_REGISTERED_USER_ID
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedValidationService
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NIDIdentifierServiceTest {
    private lateinit var logger: NIDLogWrapper
    private lateinit var neruoID: NeuroID
    private lateinit var identifierService: NIDIdentifierService
    private lateinit var validationService: NIDValidationService

    private val goodUID = "good_UID"
    private val badUID = "bad UID @#!"

    @Before
    fun setup() {
        logger = getMockedLogger()
        neruoID = getMockedNeuroID()
        validationService = getMockedValidationService()

        identifierService =
            NIDIdentifierService(
                logger = logger,
                neruoID,
                validationService,
            )

        NeuroID._isSDKStarted = false
    }

    @After
    fun teardown() {
        neruoID.registeredUserID = ""
        neruoID.userID = ""
        unmockkAll()
    }

//    getOriginResult
    @Test
    fun test_getOriginResult_CUSTOMER_SET_OK() {
        val sessionID = "gasdgasdgdsgds"
        val result = identifierService.getOriginResult(sessionID, true, true)
        Assert.assertEquals(result.origin, NID_ORIGIN_CUSTOMER_SET)
        Assert.assertEquals(result.originCode, NID_ORIGIN_CODE_CUSTOMER)
        Assert.assertEquals(result.sessionID, sessionID)
    }

    @Test
    fun test_getOriginResult_CUSTOMER_SET_FAIL() {
        val badSessionID = "gasdgas dgdsgds"
        val result = identifierService.getOriginResult(badSessionID, false, true)
        Assert.assertEquals(result.origin, NID_ORIGIN_CUSTOMER_SET)
        Assert.assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
    }

    @Test
    fun test_getOriginResult_CUSTOMER_SET_EMPTY_SESSION_ID() {
        val emptySessionID = ""
        val result = identifierService.getOriginResult(emptySessionID, true, false)
        Assert.assertEquals(result.origin, NID_ORIGIN_NID_SET)
        Assert.assertEquals(result.originCode, NID_ORIGIN_CODE_NID)
    }

    @Test
    fun test_getOriginResult_NID_SET_EMPTY_SESSION_ID() {
        val emptySessionID = ""
        val result = identifierService.getOriginResult(emptySessionID, false, false)
        Assert.assertEquals(result.origin, NID_ORIGIN_NID_SET)
        Assert.assertEquals(result.originCode, NID_ORIGIN_CODE_FAIL)
    }

    //    sendOriginEvent
    @Test
    fun test_sendOriginEvent() {
        NeuroID._isSDKStarted = false
        val originResult =
            SessionIDOriginResult(
                "testOrigin",
                "testCode",
                "sessionID",
            )

        identifierService.sendOriginEvent(originResult)

        verifyCaptureEvent(
            neruoID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionIdCode",
            v = originResult.originCode,
        )

        verifyCaptureEvent(
            neruoID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionIdSource",
            v = originResult.origin,
        )

        verifyCaptureEvent(
            neruoID,
            SET_VARIABLE,
            queuedEvent = true,
            key = "sessionId",
            v = "'${originResult.sessionID}'",
        )
    }

    //    setGenericUserID
    @Test
    fun test_setGenericUserID_invalid() {
        every { validationService.validateUserID(any()) } returns false

        val validID =
            identifierService.setGenericUserID(
                SET_USER_ID,
                badUID,
                false,
            )

        Assert.assertFalse(validID)

        verifyCaptureEvent(
            neruoID,
            SET_VARIABLE,
            3,
        )
    }

    @Test
    fun test_setGenericUserID_valid() {
        every { validationService.validateUserID(any()) } returns true

        val validID =
            identifierService.setGenericUserID(
                SET_USER_ID,
                goodUID,
                false,
            )

        assert(validID)

        verifyCaptureEvent(
            neruoID,
            SET_VARIABLE,
            3,
        )

        verifyCaptureEvent(
            neruoID,
            SET_USER_ID,
            1,
            uid = goodUID,
        )
    }

    //    getUserID
    @Test
    fun test_getUserID() {
        every { neruoID.userID } returns goodUID

        assert(identifierService.getUserID() == goodUID)
    }

    //    setUserID
    @Test
    fun test_setUserId_not_empty() {
        every { validationService.validateUserID(any()) } returns true

        val result = identifierService.setUserID(goodUID, false)

        Assert.assertTrue(result)

        verify(exactly = 1) {
            neruoID.userID = goodUID
        }

        verifyCaptureEvent(
            neruoID,
            SET_USER_ID,
            1,
            queuedEvent = true,
            uid = goodUID,
        )
    }

    //    getRegisteredUserID
    @Test
    fun test_getRegisteredUserID() {
        every { neruoID.registeredUserID } returns goodUID

        assert(identifierService.getRegisteredUserID() == goodUID)
    }

    //    setRegisteredUserID
    @Test
    fun test_setRegisteredUserId_success() {
        every { validationService.validateUserID(any()) } returns true

        val result = identifierService.setRegisteredUserID(goodUID)

        Assert.assertTrue(result)
        verify(exactly = 1) {
            neruoID.registeredUserID = goodUID
        }

        verifyCaptureEvent(
            neruoID,
            SET_REGISTERED_USER_ID,
            1,
            queuedEvent = true,
            uid = goodUID,
        )
    }

    @Test
    fun test_setRegisteredUserId_failure_existingValue() {
        every { neruoID.registeredUserID } returns goodUID

        val result = identifierService.setRegisteredUserID("")
        Assert.assertFalse(result)

        verify(exactly = 0) {
            neruoID.registeredUserID = any()
        }
        verifyCaptureEvent(
            neruoID,
            LOG,
            1,
            level = "warn",
        )
    }
}
