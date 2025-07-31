package com.neuroid.tracker.service

import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CONFIG_CACHED
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.getMockedHTTPService
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedValidationService
import com.neuroid.tracker.models.NIDLinkedSiteOption
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.RandomGenerator
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Error
import java.util.Calendar

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
        neuroID = getMockedNeuroID()
        dispatcher = neuroID.dispatcher
        logger = getMockedLogger()
        httpService = getMockedHTTPService()
        validationService = getMockedValidationService()
        configService =
            NIDConfigService(
                dispatcher,
                logger,
                neuroID,
                httpService,
                validationService,
                configRetrievalCallback = {
                    callbackCalled = true
                },
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
            configService.retrieveConfig()

            assert(!configService.cacheSetWithRemote)
            assert(callbackCalled)
        }

    // retrieveConfigCoroutine

    /***
     * This test will verify that the callback used in the HTTPService fires and works as expected
     *
     * NOTE: The callback uses other methods inside the ConfigService class, those methods are NOT
     *       extensively tested here, but rather in their own tests
     */
    @Test
    fun test_retrieveConfigCoroutine_success() {
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5L
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar

        val remoteConfig = NIDRemoteConfig(siteID = "TEST_SITE", callInProgress = false)
        val randomGenerator = mockk<RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 50.0
        httpService =
            getMockedHTTPService(
                true,
                200,
                remoteConfig,
            )
        configService =
            NIDConfigService(
                dispatcher,
                logger,
                neuroID,
                httpService,

                validationService,
            )

        var completionRun = false
        configService.retrieveConfigCoroutine {
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

        val remoteConfig = NIDRemoteConfig(siteID = "TEST_SITE", callInProgress = false)
        val randomGenerator = mockk<RandomGenerator>()

        httpService =
            getMockedHTTPService(
                true,
                200,
                remoteConfig,
            )
        configService =
            NIDConfigService(
                dispatcher,
                logger,
                neuroID,
                httpService,
                randomGenerator = randomGenerator,
                validationService = validationService
            )

        val t = NIDRemoteConfig(
            linkedSiteOptions =
            hashMapOf(
                "form_testa123" to NIDLinkedSiteOption(10),
                "form_testa124" to NIDLinkedSiteOption(50),
                "form_testa125" to NIDLinkedSiteOption(0),
                "form_testa126" to NIDLinkedSiteOption(100),
            ),
            siteID = "form_zappa345",
            sampleRate = 40,
        )


        // test roll 30
        every { randomGenerator.getRandom(any()) } returns 30.0
        configService.initSiteIDSampleMap(t)
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
        configService.initSiteIDSampleMap(t)
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
        configService.initSiteIDSampleMap(t)
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
        configService.initSiteIDSampleMap(t)
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
                neuroID,
                httpService,
                validationService,
            )

        var completionRun = false
        configService.retrieveConfigCoroutine {
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

        configService.captureConfigEvent(remoteConfig)

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
                neuroID,
                httpService,
                validationService,
                gsonMock,
            )

        val remoteConfig = NIDRemoteConfig()

        configService.captureConfigEvent(remoteConfig)

        verifyCaptureEvent(
            neuroID,
            LOG,
            1,
            m = "Failed to parse config",
            level = "ERROR",
        )
    }
}
