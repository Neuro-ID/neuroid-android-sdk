package com.neuroid.tracker.service

import com.neuroid.tracker.models.NIDLinkedSiteOption
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.RandomGenerator
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Test

class NIDSamplingServiceTest {
    @Test
    fun testParentNoThrottle() {
        testSampleService("form_zappa345", 10.0, true)
    }

    @Test
    fun testParentThrottle() {
        // ENG-8324
//        testSampleService("form_zappa345", 70.0, false)
        testSampleService("form_zappa345", 70.0, true)
    }

    @Test
    fun testLinkedSiteIDThrottle() {
        // ENG-8324
//        testSampleService("form_testa123", 10.0, false)
        testSampleService("form_testa123", 10.0, true)
    }

    @Test
    fun testLinkedSiteIDNoThrottle() {
        testSampleService("form_testa124", 30.0, true)
    }

    @Test
    fun testEmptySiteID() {
        testSampleService("", 10.0, true)
    }

    @Test
    fun testNullSiteID() {
        testSampleService(null, 10.0, true)
    }

    fun testSampleService(
        siteID: String?,
        randomNumber: Double,
        expectedValue: Boolean,
    ) {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.w(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        every { logger.i(any(), any()) } just runs

        val randomGenerator = mockk<RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns randomNumber
        val configService = mockk<NIDConfigService>()
        every { configService.configCache } returns
            NIDRemoteConfig(
                linkedSiteOptions =
                    hashMapOf(
                        "form_testa123" to NIDLinkedSiteOption(10),
                        "form_testa124" to NIDLinkedSiteOption(50),
                    ),
                siteID = "form_zappa345",
                sampleRate = 40,
            )

        val sampleService = NIDSamplingService(logger, randomGenerator, configService)
        sampleService.updateIsSampledStatus(siteID)
        assert(sampleService.isSessionFlowSampled() == expectedValue)
    }
}
