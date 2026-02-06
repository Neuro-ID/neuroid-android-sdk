package com.neuroid.tracker.service

import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CLEAR_SAMPLE_SITE_ID_MAP
import com.neuroid.tracker.events.CONFIG_CACHED
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.getMockedHTTPService
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedRandomNumberGenerator
import com.neuroid.tracker.getMockedTime
import com.neuroid.tracker.getMockedValidationService
import com.neuroid.tracker.models.NIDLinkedSiteOption
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
import com.neuroid.tracker.utils.RandomGenerator
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Error
import java.util.Calendar
import kotlin.math.log

/**
 * NOTE: Ignoring testing `retrieveOrRefreshCache` because it just calls other functions
 *       that are already tested
 */
class NIDConfigServiceTest {
    private lateinit var neuroID: NeuroID
    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var logger: NIDLogWrapper
    private lateinit var httpService: HttpService
    private lateinit var configService: NIDConfigService
    private lateinit var validationService: NIDValidationService

    private var callbackCalled: Boolean = false

    @Before
    fun setup() {
        neuroID = getMockedNeuroID(mockedRandomNumberGenerator = getMockedRandomNumberGenerator(50.0), mockNIDTime = getMockedTime(5L))
        dispatcher = neuroID.dispatcher
        logger = getMockedLogger()
        httpService = getMockedHTTPService(true)
        validationService = getMockedValidationService()
        configService =
            NIDConfigService(
                dispatcher,
                logger,
                httpService,
                validationService,
                configRetrievalCallback = {
                    callbackCalled = true
                }
            )
    }

    @After
    fun teardown() {
        callbackCalled = false
        unmockkAll()
    }

    // retrieveConfig

    /***
     * This function tests if there is an issue with the client key which prevents the config
     * from ever being retrieved
     *
     * NOTE: Because this function uses a coroutine and our mocking immediately completes the
     *       coroutine call we will NOT test a success case of this function. However the inner
     *       function of the coroutine is broken into a separate function in order to test.
     *       see `retrieveConfigCoroutine` tests.
     */
    @Test
    fun test_retrieveConfig_failure() =
        runBlocking {
            // Given
            every { validationService.verifyClientKeyExists(any()) } returns false
            configService.cacheSetWithRemote = true

            // When
            configService.retrieveConfig(
                neuroID
            )

            assert(!configService.cacheSetWithRemote)
            assert(callbackCalled)
        }

    // retrieveConfigCoroutine

    @Test
    fun test_clearSiteSampleMap() {
        configService = NIDConfigService(dispatcher, logger, httpService, validationService)
        configService.clearSiteIDSampleMap(neuroID)
        assert(configService.siteIDSampleMap.isEmpty())
        verifyCaptureEvent(
            neuroID,
            queuedEvent = true,
            eventType = CLEAR_SAMPLE_SITE_ID_MAP,
        )
    }

    /***
     * This test will verify that the callback used in the HTTPService fires and works as expected
     *
     * NOTE: The callback uses other methods inside the ConfigService class, those methods are NOT
     *       extensively tested here, but rather in their own tests
     */
    @Test
    fun test_retrieveConfigCoroutine_success() {
        val nidTime = mockk<NIDTime>()
        every { nidTime.getCurrentTimeMillis()} returns 5L
        val remoteConfig = NIDRemoteConfig(siteID = "TEST_SITE", callInProgress = false)
        val randomGenerator = mockk<RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 50.0
        httpService =
            getMockedHTTPService(
                true,
                200,
                remoteConfig,
            )
        configService = NIDConfigService(
            dispatcher,
            logger,
            httpService,
            validationService,
            nidTime = nidTime)
        var completionRun = false
        configService.retrieveConfigCoroutine(neuroID) {
            completionRun = true
        }

        assert(completionRun)
        assert(configService.cacheSetWithRemote)
        assert(configService.cacheCreationTime == 5L)
        assert(configService.configCache == remoteConfig)

        verifyCaptureEvent(
            neuroID,
            CONFIG_CACHED,
            1,
        )
    }

    @Test
    fun test_initSiteIDSampleMap() {
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5L
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar

        val remoteConfig = NIDRemoteConfig(
            linkedSiteOptions =
            hashMapOf(
                "form_testa123" to NIDLinkedSiteOption(10),
                "form_testa124" to NIDLinkedSiteOption(50),
                "form_testa125" to NIDLinkedSiteOption(0),
                "form_testa126" to NIDLinkedSiteOption(100),
            ),
            siteID = "form_zappa345",
            sampleRate = 40
        )
        val randomGenerator = mockk<RandomGenerator>()

        httpService =
            getMockedHTTPService(
                true,
                200,
                remoteConfig,
            )
        val vs = mockk<NIDValidationService>()
        every { vs.verifyClientKeyExists(any()) } returns true

        configService =
            NIDConfigService(
                dispatcher,
                logger,
                httpService,
                vs,
                randomGenerator = randomGenerator
            )

        // test roll 30
        every { randomGenerator.getRandom(any()) } returns 30.0
        every {neuroID.randomGenerator} returns randomGenerator
        configService.retrieveConfig(neuroID)
        configService.initSiteIDSampleMap(neuroID, remoteConfig)

        for (linkedSiteID in remoteConfig.linkedSiteOptions.keys) {
            remoteConfig.linkedSiteOptions[linkedSiteID]?.let  {
                if (it.sampleRate == 0) {
                    assert(configService.siteIDSampleMap[linkedSiteID] == false)
                } else {
                    assert(configService.siteIDSampleMap[linkedSiteID] == 30.0 <= it.sampleRate)
                }
            }
        }

        configService.siteIDSampleMap.forEach { (key, value) ->
            if (key == "form_testa123") {
                assert(!value)
            }
            if (key == "form_testa124") {
                assert(value)
            }
            if (key == "form_testa125") {
                assert(!value)
            }
            if (key == "form_testa126") {
                assert(value)
            }
            if (key == "form_zappa345") {
                assert(value)
            }
        }
        // test roll 50
        every { randomGenerator.getRandom(any()) } returns 50.0
        every {neuroID.randomGenerator} returns randomGenerator
        configService.retrieveConfig(neuroID)
        configService.initSiteIDSampleMap(neuroID, remoteConfig)
        for (linkedSiteID in remoteConfig.linkedSiteOptions.keys) {
            remoteConfig.linkedSiteOptions[linkedSiteID]?.let  {
                if (it.sampleRate == 0) {
                    assert(configService.siteIDSampleMap[linkedSiteID] == false)
                } else {
                    assert(configService.siteIDSampleMap[linkedSiteID] == 50.0 <= it.sampleRate)
                }
            }
        }
        configService.siteIDSampleMap.forEach { (key, value) ->
            if (key == "form_testa123") {
                assert(!value)
            }
            if (key == "form_testa124") {
                assert(value)
            }
            if (key == "form_testa125") {
                assert(!value)
            }
            if (key == "form_testa126") {
                assert(value)
            }
            if (key == "form_zappa345") {
                assert(!value)
            }
        }

        // test roll 100
        every { randomGenerator.getRandom(any()) } returns 100.0
        every {neuroID.randomGenerator} returns randomGenerator
        configService.retrieveConfig(neuroID)
        configService.initSiteIDSampleMap(neuroID, remoteConfig)
        for (linkedSiteID in remoteConfig.linkedSiteOptions.keys) {
            remoteConfig.linkedSiteOptions[linkedSiteID]?.let  {
                if (it.sampleRate == 0) {
                    assert(configService.siteIDSampleMap[linkedSiteID] == false)
                } else {
                    assert(configService.siteIDSampleMap[linkedSiteID] == 100.0 <= it.sampleRate)
                }
            }
        }
        configService.siteIDSampleMap.forEach { (key, value) ->
            if (key == "form_testa123") {
                assert(!value)
            }
            if (key == "form_testa124") {
                assert(!value)
            }
            if (key == "form_testa125") {
                assert(!value)
            }
            if (key == "form_testa126") {
                assert(value)
            }
            if (key == "form_zappa345") {
                assert(!value)
            }
        }

        // test roll 0
        every { randomGenerator.getRandom(any()) } returns 0.0
        every {neuroID.randomGenerator} returns randomGenerator
        configService.retrieveConfig(neuroID)
        configService.initSiteIDSampleMap(neuroID, remoteConfig)
        for (linkedSiteID in remoteConfig.linkedSiteOptions.keys) {
            remoteConfig.linkedSiteOptions[linkedSiteID]?.let  {
                if (it.sampleRate == 0) {
                    assert(configService.siteIDSampleMap[linkedSiteID] == false)
                } else {
                    assert(configService.siteIDSampleMap[linkedSiteID] == 0.0 <= it.sampleRate)
                }
            }
        }
        configService.siteIDSampleMap.forEach { (key, value) ->
            if (key == "form_testa123") {
                assert(value)
            }
            if (key == "form_testa124") {
                assert(value)
            }
            if (key == "form_testa125") {
                assert(!value)
            }
            if (key == "form_testa126") {
                assert(value)
            }
            if (key == "form_zappa345") {
                assert(value)
            }
        }

        // unknown form, should be true for all forms
        every { randomGenerator.getRandom(any()) } returns 100.0
        every {neuroID.randomGenerator} returns randomGenerator
        configService.retrieveConfig(neuroID)
        configService.updateIsSampledStatus(getMockedNeuroID(), "hgjksdahgkldashlg")
        assert(configService.isSessionFlowSampled())
    }

    @Test
    fun test_malformedHTTPResponse() {
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5L
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar

        val randomGenerator = mockk<RandomGenerator>()

        httpService =
            getMockedHTTPService(
                false,
                400,
                "gdsgdfgdfshdfshdf"
            )

        val mockValidationService = mockk<NIDValidationService>()
        every { mockValidationService.verifyClientKeyExists(any()) } returns true

        configService =
            NIDConfigService(
                dispatcher,
                logger,
                httpService,
                mockValidationService,
                randomGenerator = randomGenerator

            )
        configService.retrieveConfig(neuroID)
        assert(configService.siteIDSampleMap.isEmpty())
        configService.updateIsSampledStatus(getMockedNeuroID(), "formTest_1234")
        assert(configService.isSessionFlowSampled())
    }

    /***
     * This test will verify that the callback used in the HTTPService fires and works as expected
     *
     * NOTE: The callback uses other methods inside the ConfigService class, those methods are NOT
     *       extensively tested here, but rather in their own tests
     */
    @Test
    fun test_retrieveConfigCoroutine_failure() {
        val remoteConfig = NIDRemoteConfig(siteID = "TEST_SITE", callInProgress = false)

        httpService =
            getMockedHTTPService(
                false,
                400,
                "ERROR",
            )
        configService =
            NIDConfigService(
                dispatcher,
                logger,
                httpService,
                validationService
            )

        var completionRun = false
        configService.retrieveConfigCoroutine(neuroID) {
            completionRun = true
        }

        assert(completionRun)
        assert(!configService.cacheSetWithRemote)
        assert(configService.configCache != remoteConfig)

        verifyCaptureEvent(
            neuroID,
            CONFIG_CACHED,
            1,
        )
    }

    // setCache
    @Test
    fun test_setCache() {
        val remoteConfig = NIDRemoteConfig(callInProgress = false)

        assert(configService.configCache.callInProgress)

        configService.setCache(remoteConfig)

        assert(!configService.configCache.callInProgress)
    }

    // expiredCache
    @Test
    fun test_expiredCache_true() {
        configService.cacheSetWithRemote = false

        assert(configService.expiredCache())
    }

    @Test
    fun test_expiredCache_false() {
        configService.cacheSetWithRemote = true

        assert(!configService.expiredCache())
    }

    // retrieveOrRefreshCache

    // captureConfigEvent
    @Test
    fun test_captureConfigEvent_success() {
        val remoteConfig = NIDRemoteConfig()
        val neuroID = mockk<NeuroID>(relaxed = true)
        val nidTime = mockk<NIDTime>()
        every { nidTime.getCurrentTimeMillis() } returns 1000
        every { neuroID.nidTime } returns nidTime
        every { neuroID.captureEvent(any(), any(), 1000) } just Runs
        
        neuroID.nidTime = nidTime
        configService.captureConfigEvent(neuroID, remoteConfig)

        verifyCaptureEvent(
            neuroID,
            CONFIG_CACHED,
            1,
        )
    }

    @Test
    fun test_captureConfigEvent_failure() {
        val gsonMock = mockk<Gson>()
        every { gsonMock.toJson(any()) } throws Error("NOPE")

        configService =
            NIDConfigService(
                dispatcher,
                logger,
                httpService,
                validationService,
                gsonMock)

        val remoteConfig = NIDRemoteConfig()

        configService.captureConfigEvent(neuroID, remoteConfig)

        verifyCaptureEvent(
            neuroID,
            LOG,
            1,
            m = "Failed to parse config",
            level = "ERROR",
        )
    }
}
