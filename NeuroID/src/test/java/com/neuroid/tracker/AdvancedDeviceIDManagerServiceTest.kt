package com.neuroid.tracker

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.fingerprintjs.android.fpjs_pro.Error
import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.fingerprintjs.android.fpjs_pro.FingerprintJSProResponse
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.extensions.getADVSignal
import com.neuroid.tracker.models.ADVKeyFunctionResponse
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.ADVNetworkService
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.service.AdvancedDeviceIDManagerService
import com.neuroid.tracker.service.ConfigService
import com.neuroid.tracker.service.NIDAdvancedDeviceNetworkService
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration

class AdvancedDeviceIDManagerServiceTest {
    /*
     TESTS
     */
    @Test
    fun testGetADVSignal_is_sampled_true() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey("")
        val mockedNeuroID = mocks.get("mockedNeuroID") as NeuroID
        val mockedNIDConfigService = mockk<ConfigService>()
        every { mockedNIDConfigService.isSessionFlowSampled() } returns true
        every { mockedNeuroID.configService } returns mockedNIDConfigService
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        getADVSignal(advancedDeviceIDManagerService, "dummy_key", mockedNeuroID, Dispatchers.Unconfined)
        verify(exactly = 1) { advancedDeviceIDManagerService.getCachedID() }
    }

    @Test
    fun testGetADVSignal_is_sampled_false() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey("")
        val mockedNeuroID = mocks.get("mockedNeuroID") as NeuroID
        val mockedNIDConfigService = mockk<ConfigService>()
        every { mockedNIDConfigService.isSessionFlowSampled() } returns false
        every { mockedNeuroID.configService } returns mockedNIDConfigService
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        getADVSignal(advancedDeviceIDManagerService, "dummy_key", mockedNeuroID, Dispatchers.Unconfined)
        verify(exactly = 0) { advancedDeviceIDManagerService.getCachedID() }
    }

    //    getCachedID
    @Test
    fun testGetCachedID_not_stored() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey("")
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify(exactly = 1) {
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_default_value() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey()
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify(exactly = 1) {
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_expired_id() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey("{\"key\":\"testingExp\", \"exp\":0}")
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify(exactly = 1) {
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_valid_cache() {
        val keyValue = "ValidKey"
        val mocks =
            buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey(
                "{\"key\":\"$keyValue\", \"exp\":${System.currentTimeMillis() + (1 * 60 * 60 * 1000)}}",
            ) { e: NIDEventModel ->
                assert(e.type == ADVANCED_DEVICE_REQUEST) { "Expected event type to be ${ADVANCED_DEVICE_REQUEST}, found ${e.type}" }
                assert(e.rid == keyValue) { "Expected event requestID to be $keyValue, found ${e.rid}" }
                assert(e.c == true) { "Expected event c value to be true, found false" }
                assert(e.l == 0L) { "Expected event l value to be 0, found ${e.l}" }
                assert(e.ct == "wifi") { "Expected event ct value to be wifi, found ${e.ct}" }
            }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper
        val mockedNID = mocks["mockedNeuroID"] as NeuroID
        every { mockedNID.networkConnectionType } returns "wifi"

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(cachedID)
        verifyCaptureEvent(mockedNID, ADVANCED_DEVICE_REQUEST, 1)
        verify(exactly = 1) {
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
            mockedLogger.d(msg = "Retrieving Request ID for Advanced Device Signals from cache: $keyValue")
        }
    }

    //    getRemoteID
    @Test
    fun testGetRemoteID_no_nid_response() {
        val errorMessage = "Network Error"
        val mocks =
            buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey(
                networkServiceResult = Triple("", false, errorMessage),
            ) { e: NIDEventModel ->
                assert(e.type == LOG) { "Expected event type to be $LOG, found ${e.type}" }
                assert(e.level == "error") { "Expected event level to be ${"error"}, found ${e.level}" }
                assert(e.m == errorMessage) { "Expected event m value to be $errorMessage, found ${e.m}" }
            }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedNID = mocks["mockedNeuroID"] as NeuroID
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper

        advancedDeviceIDManagerService.getRemoteID(
            "testKey",
            Dispatchers.Unconfined,
            10,
        )

        verifyCaptureEvent(mockedNID, LOG, 1)
        verify(exactly = 1) {
            mockedLogger.e(msg = "Failed to get API key from NeuroID: $errorMessage")
        }
    }

    @Test
    fun testGetRemoteID_fpjs_failure() {
        val errorMessage = "FPJS Failure"
        val fullErrorMessage =
            "Reached maximum number of retries (${NIDAdvancedDeviceNetworkService.RETRY_COUNT}) to get Advanced Device Signal Request ID:$errorMessage"

        val mocks =
            buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey(
                networkServiceResult = Triple("", true, ""),
                fpjsResponse = Triple(null, errorMessage, null),
            ) { e: NIDEventModel ->
                assert(e.type == LOG) { "Expected event type to be $LOG, found ${e.type}" }
                assert(e.level == "error") { "Expected event level to be ${"error"}, found ${e.level}" }
                assert(e.m == fullErrorMessage) { "Expected event m value to be $fullErrorMessage, found ${e.m}" }
            }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper
        val mockedNeuroID = mocks["mockedNeuroID"] as NeuroID

        // need to let the job complete so the verification step can commence.
        // we do this with an unconfined dispatcher.
        val job =
            advancedDeviceIDManagerService.getRemoteID(
                "testKey",
                dispatcher = Dispatchers.Unconfined,
                delay = 10,
            )

        job?.invokeOnCompletion {
            verify(exactly = 1) {
                mockedLogger.d(msg = "Error retrieving Advanced Device Signal Request ID:$errorMessage: 1")
                mockedLogger.d(msg = "Error retrieving Advanced Device Signal Request ID:$errorMessage: 2")
                mockedLogger.d(msg = "Error retrieving Advanced Device Signal Request ID:$errorMessage: 3")
                mockedLogger.e(msg = "Reached maximum number of retries (3) to get Advanced Device Signal Request ID: FPJS Failure")
                mockedNeuroID.captureEvent(
                    queuedEvent = true,
                    type = "LOG",
                    ts = 0L,
                    level = "error",
                    m = "Reached maximum number of retries (3) to get Advanced Device Signal Request ID: FPJS Failure",
                )
                mockedNeuroID.captureEvent(
                    queuedEvent = true,
                    type ="ADVANCED_DEVICE_REQUEST_FAILED",
                    ts = 0L,
                    m = "Reached maximum number of retries (3) to get Advanced Device Signal Request ID: FPJS Failure"
                )
            }
        }
        unmockkAll()
        unmockkStatic(Calendar::class)
    }

    @Test
    fun testGetRemoteID_fpjs_success_server_set_key() =
        runTest(timeout = Duration.parse("120s")) {
            val validRID = "Valid RID Key"

            val mocks =
                buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey(
                    networkServiceResult = Triple("", true, ""),
                    fpjsResponse = Triple(validRID, null, "sealedResult"),
                ) { e: NIDEventModel ->
                    assert(e.type == ADVANCED_DEVICE_REQUEST) { "Expected event type to be ${ADVANCED_DEVICE_REQUEST}, found ${e.type}" }
                    assert(e.rid == validRID) { "Expected event requestID to be $validRID, found ${e.rid}" }
                    assert(e.c == false) { "Expected event c value to be false, found true" }
                    assert(e.l != 0L) { "Expected event l value to be !=0, found ${e.l}" }
                    assert(e.ct == "wifi") { "Expected event c value to be wifi, found ${e.ct}" }
                    assert(e.scr  == "sealedResult") {"Expected event scr value to be sealedResult, found ${e.scr}"}
                }
            val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
            val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults
            val mockedNID = mocks["mockedNeuroID"] as NeuroID
            val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper
            mockkStatic(Calendar::class)
            every { Calendar.getInstance().timeInMillis } returns 0

            // need to let the job complete so the verification step can commence.
            // we do this with an unconfined dispatcher.
            val job = advancedDeviceIDManagerService.getRemoteID("testKey", Dispatchers.Unconfined, 10)

            job?.invokeOnCompletion {
                verify(exactly = 1) {
                    mockedLogger.d(msg = "Generating Request ID for Advanced Device Signals: $validRID")
                    mockedNID.captureEvent(queuedEvent = true, type = ADVANCED_DEVICE_REQUEST, rid = any(), ts = any(), c = false, l = 0, scr=any(), ct = any(), m="server retrieved FPJS key")
                    mockedLogger.d(msg = "Caching Request ID: $validRID")
                    mockedSharedPreferences.putString(AdvancedDeviceIDManager.NID_RID, any())
                }
            }
        }

    @Test
    fun chooseUrl() {
        val mocks = buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey()
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService

        val proxyEnabledUrl = (advancedDeviceIDManagerService as AdvancedDeviceIDManager).chooseUrl(
            useAdvancedDeviceProxy = true
        )
        assert(proxyEnabledUrl == "https://dn.neuroid.cloud/iynlfqcb0t") {
            "Expected proxy endpoint URL when proxy is enabled, found $proxyEnabledUrl"
        }

        val standardUrl = (advancedDeviceIDManagerService as AdvancedDeviceIDManager).chooseUrl(
            useAdvancedDeviceProxy = false
        )
        assert(standardUrl == "https://advanced.neuro-id.com") {
            "Expected standard endpoint URL when proxy is disabled, found $standardUrl"
        }
    }

    @Test
    fun testGetRemoteID_fpjs_success_user_set_key() =
        runTest(timeout = Duration.parse("120s")) {
            val validRID = "Valid RID Key"

            val mocks =
                buildAdvancedDeviceIDManagerService_userSetAdvancedKey(
                    networkServiceResult = Triple("", true, ""),
                    fpjsResponse = Triple(validRID, null, "sealedResult"),
                ) { e: NIDEventModel ->
                    assert(e.type == ADVANCED_DEVICE_REQUEST) { "Expected event type to be ${ADVANCED_DEVICE_REQUEST}, found ${e.type}" }
                    assert(e.rid == validRID) { "Expected event requestID to be $validRID, found ${e.rid}" }
                    assert(e.c == false) { "Expected event c value to be false, found true" }
                    assert(e.l != 0L) { "Expected event l value to be !=0, found ${e.l}" }
                    assert(e.ct == "wifi") { "Expected event c value to be wifi, found ${e.ct}" }
                    assert(e.scr == "sealedResult") {"Expected event scr value to be sealedResult, found ${e.scr}"}
                }
            val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
            val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults
            val mockedNID = mocks["mockedNeuroID"] as NeuroID
            val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper
            mockkStatic(Calendar::class)
            every { Calendar.getInstance().timeInMillis } returns 0

            // need to let the job complete so the verification step can commence.
            // we do this with an unconfined dispatcher.
            val job = advancedDeviceIDManagerService.getRemoteID("testKey", Dispatchers.Unconfined, 10)

            job?.invokeOnCompletion {
                verify(exactly = 1) {
                    mockedLogger.d(msg = "Generating Request ID for Advanced Device Signals: $validRID")
                    mockedNID.captureEvent(queuedEvent = true, type = ADVANCED_DEVICE_REQUEST, rid = any(), ts = any(), c = false, l = any(), scr="sealedResult", ct = any(), m="user entered FPJS key")
                    mockedLogger.d(msg = "Caching Request ID: $validRID")
                    mockedSharedPreferences.putString(AdvancedDeviceIDManager.NID_RID, any())
                }
            }
        }

    /*
        Mocking Functions
     */
    private fun buildAdvancedDeviceIDManagerService_noUserSetAdvancedKey(
        sharedPrefGetValue: String = "{\"key\":\"testingExp\", \"exp\":0}",
        networkServiceResult: Triple<String, Boolean, String> = Triple("", false, ""),
        fpjsResponse: Triple<String?, String?, String?> = Triple(null, null, null),
        advancedDeviceKey: String? = null,
        useAdvancedDeviceProxy: Boolean = false,
        saveEventTest: (e: NIDEventModel) -> Unit = {},
    ): Map<String, Any> {
        val mockedNidTime = mockk<NIDTime>()
        every { mockedNidTime.getCurrentTimeMillis() } returns 0L
        val mockedNeuroID = getMockedNeuroID()
        val mockedApplication = getMockedApplication()
        val mockedSharedPreferences = getMockedSharedPrefs(AdvancedDeviceIDManager.NID_RID, sharedPrefGetValue)
        val mockedLogger = getMockedLogger()
        val mockedDataStore = getMockedDatastoreManager(saveEventTest)
        val mockedNetworkService =
            getMockADVNetworkService(
                networkServiceResult.first,
                networkServiceResult.second,
                networkServiceResult.third,
            )
        val mockedFPJSClient = getMockedFPJSClient(fpjsResponse.first, fpjsResponse.second, fpjsResponse.third)

        val advancedDeviceIDManagerService =
            AdvancedDeviceIDManager(
                mockedApplication,
                mockedLogger,
                mockedSharedPreferences,
                mockedNeuroID,
                mockedNetworkService,
                "",
                "",
                getMockedConfigService(),
                advancedDeviceKey,
                mockedFPJSClient,
                useAdvancedDeviceProxy = useAdvancedDeviceProxy,
                mockedNidTime
            )

        return mapOf(
            "advancedDeviceIDManagerService" to advancedDeviceIDManagerService,
            "mockedNeuroID" to mockedNeuroID,
            "mockedNetworkService" to mockedNetworkService,
            "mockedDataStore" to mockedDataStore,
            "mockedLogger" to mockedLogger,
            "mockedSharedPreferences" to mockedSharedPreferences,
            "mockedApplication" to mockedApplication,
            "mockedFPJSClient" to mockedFPJSClient,
        )
    }

    /*
        Mocking Functions
     */
    private fun buildAdvancedDeviceIDManagerService_userSetAdvancedKey(
        sharedPrefGetValue: String = "{\"key\":\"testingExp\", \"exp\":0}",
        networkServiceResult: Triple<String, Boolean, String> = Triple("", false, ""),
        fpjsResponse: Triple<String?, String?, String?> = Triple(null, null, null),
        advancedDeviceKey: String? = "gsagasdgasdgsdg",
        useAdvancedDeviceProxy: Boolean = false,
        saveEventTest: (e: NIDEventModel) -> Unit = {},
    ): Map<String, Any> {
        val mockedNeuroID = getMockedNeuroID()
        val mockedApplication = getMockedApplication()
        val mockedSharedPreferences = getMockedSharedPrefs(AdvancedDeviceIDManager.NID_RID, sharedPrefGetValue)
        val mockedLogger = getMockedLogger()
        val mockedDataStore = getMockedDatastoreManager(saveEventTest)
        val mockedNetworkService =
            getMockADVNetworkService(
                networkServiceResult.first,
                networkServiceResult.second,
                networkServiceResult.third,
            )
        val mockedFPJSClient = getMockedFPJSClient(fpjsResponse.first, fpjsResponse.second, fpjsResponse.third)

        val advancedDeviceIDManagerService =
            AdvancedDeviceIDManager(
                mockedApplication,
                mockedLogger,
                mockedSharedPreferences,
                mockedNeuroID,
                mockedNetworkService,
                "",
                "",
                getMockedConfigService(),
                advancedDeviceKey,
                mockedFPJSClient,
                useAdvancedDeviceProxy = useAdvancedDeviceProxy
            )

        return mapOf(
            "advancedDeviceIDManagerService" to advancedDeviceIDManagerService,
            "mockedNeuroID" to mockedNeuroID,
            "mockedNetworkService" to mockedNetworkService,
            "mockedDataStore" to mockedDataStore,
            "mockedLogger" to mockedLogger,
            "mockedSharedPreferences" to mockedSharedPreferences,
            "mockedApplication" to mockedApplication,
            "mockedFPJSClient" to mockedFPJSClient,
        )
    }

    private fun getMockedNeuroID(): NeuroID {
        val nidMock = com.neuroid.tracker.getMockedNeuroID()
        every { nidMock.networkConnectionType } returns "wifi"
        every { nidMock.captureEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(),)} just runs
        return nidMock
    }

    private fun getMockedSharedPrefs(
        key: String,
        getValue: String = "",
    ): NIDSharedPrefsDefaults {
        val sharedPrefs = mockk<NIDSharedPrefsDefaults>()
        every { sharedPrefs.putString(key, any()) } just runs
        every { sharedPrefs.getString(key, any()) } returns getValue

        return sharedPrefs
    }

    private fun getMockedDatastoreManager(saveEventTest: (e: NIDEventModel) -> Unit = {}): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } answers {
            saveEventTest(args[0] as NIDEventModel)
            mockk<Job>()
        }
        every { dataStoreManager.saveEvent(any()) } just runs
        return dataStoreManager
    }

    private fun getMockedLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        return logger
    }

    private fun getMockedApplication(): Application {
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(any()) } returns listOf()
        every { sensorManager.unregisterListener(any<NIDSensorGenListener>()) } just runs
        every { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>(), any<Int>()) }

        val application = mockk<Application>()
        every { application.getSystemService(any()) } returns sensorManager

        val sharedPreferences = mockk<SharedPreferences>()
        every { sharedPreferences.getString(any(), any()) } returns "test"
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences

        val activityManager = mockk<ActivityManager>()
        every { application.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } just runs

        return application
    }

    private fun getMockADVNetworkService(
        key: String = "",
        success: Boolean = false,
        message: String = "",
    ): ADVNetworkService {
        val mockedADVNetworkService = mockk<ADVNetworkService>()

        every { mockedADVNetworkService.getNIDAdvancedDeviceAccessKey(any(), any(), any()) } returns
            ADVKeyFunctionResponse(
                key,
                success,
                message,
            )
        return mockedADVNetworkService
    }

    private fun getMockedFPJSClient(
        successResponse: String?,
        errorResponse: String?,
        sealedResult: String?
    ): FingerprintJS {
        val mockedFPJSClient = mockk<FingerprintJS>()
        every { mockedFPJSClient.getVisitorId(tags = ofType<Map<String, Any>>(), listener = any(), errorListener = any()) }.answers {
            if (successResponse != null) {
                val successListener = args[1] as (FingerprintJSProResponse) -> Unit
                val mockSuccessResponse = mockk<FingerprintJSProResponse>()
                every {mockSuccessResponse.sealedResult} returns sealedResult
                every {mockSuccessResponse.requestId} returns successResponse
                successListener(mockSuccessResponse)
            }
            if (errorResponse != null) {
                // args[0] = tags, args[1] = listener, args[2] = errorListener
                val errorListener = args[2] as (Error) -> Unit
                val mockSuccessResponse = mockk<Error>()
                every { mockSuccessResponse.description } returns errorResponse
                errorListener(mockSuccessResponse)
            }
        }

        return mockedFPJSClient
    }
}
