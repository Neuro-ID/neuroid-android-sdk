package com.neuroid.tracker.service

import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NIDValidationServiceTest {
    private lateinit var logger: NIDLogWrapper
    private lateinit var validationService: NIDValidationService

    @Before
    fun setup() {
        logger = getMockedLogger()

        validationService =
            NIDValidationService(
                logger = logger,
            )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    //    validateClientKey
    @Test
    fun test_validateClientKey_bad_key() {
        val value = validationService.validateClientKey("kjjhgh")

        Assert.assertEquals(false, value)
    }

    @Test
    fun test_validateClientKey_invalid_key() {
        val value = validationService.validateClientKey("key_tert_fdffsd")

        Assert.assertEquals(false, value)
    }

    @Test
    fun test_validateClientKey_valid_key() {
        val value = validationService.validateClientKey("key_test_1235")

        Assert.assertEquals(true, value)
    }

    // verifyClientKeyExists
    @Test
    fun test_verifyClientKeyExists_success() {
        val value = validationService.verifyClientKeyExists("key_test_1235")

        Assert.assertEquals(true, value)
    }

    @Test
    fun test_verifyClientKeyExists_failure_empty() {
        val value = validationService.verifyClientKeyExists("")

        Assert.assertEquals(false, value)
    }

    @Test
    fun test_verifyClientKeyExists_failure_null() {
        val value = validationService.verifyClientKeyExists(null)

        Assert.assertEquals(false, value)
    }

    // validateSiteID
    @Test
    fun test_validateSiteID_valid() {
        val value = validationService.validateSiteID("form_abcde123")

        Assert.assertEquals(true, value)
    }

    @Test
    fun test_validateSiteID_invalid() {
        val value = validationService.validateSiteID("form_abnd")

        Assert.assertEquals(false, value)
    }

    //    validateUserId
    @Test
    fun test_validateUserID_valid() {
        val value = validationService.validateUserID("goodUserId")

        Assert.assertEquals(true, value)
    }

    @Test
    fun test_validateUserID_invalid() {
        val value = validationService.validateUserID("bad userID")

        Assert.assertEquals(false, value)
    }

    // scrubIdentifier
    @Test
    fun test_scrubIdentifier_email() {
        val value = validationService.scrubIdentifier("test@test.com")

        Assert.assertEquals("t**t@test.com", value)
    }

    @Test
    fun test_scrubIdentifier_ssn() {
        val value = validationService.scrubIdentifier("123-45-6789")

        Assert.assertEquals("***-**-****", value)
    }

    @Test
    fun test_scrubIdentifier_string() {
        val value = validationService.scrubIdentifier("testID")

        Assert.assertEquals("testID", value)
    }
}
