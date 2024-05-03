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
import com.neuroid.tracker.models.ADVKeyFunctionResponse
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.ADVNetworkService
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.service.AdvancedDeviceIDManagerService
import com.neuroid.tracker.service.NIDAdvancedDeviceNetworkService
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Job
import org.junit.Test

class AdvancedDeviceIDManagerServiceTest {
    /*
     TESTS
     */

    //    getCachedID
    @Test
    fun testGetCachedID_not_stored(){
        val mocks = buildAdvancedDeviceIDManagerService("")
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify (exactly = 1){
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_default_value(){
        val mocks = buildAdvancedDeviceIDManagerService()
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify (exactly = 1){
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_expired_id(){
        val mocks = buildAdvancedDeviceIDManagerService("{\"key\":\"testingExp\", \"exp\":0}")
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(!cachedID)
        verify (exactly = 1){
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
        }
    }

    @Test
    fun testGetCachedID_valid_cache(){
        val keyValue = "ValidKey"
        val mocks = buildAdvancedDeviceIDManagerService(
            "{\"key\":\"$keyValue\", \"exp\":${System.currentTimeMillis()+(1 * 60 * 60 * 1000)}}"
        ) { e: NIDEventModel ->
            assert(e.type == ADVANCED_DEVICE_REQUEST) { "Expected event type to be ${ADVANCED_DEVICE_REQUEST}, found ${e.type}" }
            assert(e.rid == keyValue) { "Expected event requestID to be ${keyValue}, found ${e.rid}" }
            assert(e.c == true) { "Expected event c value to be true, found false" }
        }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper
        val mockedNID = mocks["mockedNeuroID"] as NeuroID
        every {mockedNID.isWifi} returns true

        val cachedID = advancedDeviceIDManagerService.getCachedID()

        assert(cachedID)
        verifyCaptureEvent(mockedNID, 1)
        verify (exactly = 1){
            mockedSharedPreferences.getString(AdvancedDeviceIDManager.NID_RID, AdvancedDeviceIDManager.defaultCacheValue)
            mockedLogger.d(msg="Retrieving Request ID for Advanced Device Signals from cache: ${keyValue}")
        }
    }

    //    getRemoteID
    @Test
    fun testGetRemoteID_no_nid_response(){
        val errorMessage = "Network Error"
        val mocks = buildAdvancedDeviceIDManagerService(
            networkServiceResult = Triple("", false, errorMessage)
        ) { e: NIDEventModel ->
            assert(e.type == LOG) { "Expected event type to be ${LOG}, found ${e.type}" }
            assert(e.level == "error") { "Expected event level to be ${"error"}, found ${e.level}" }
            assert(e.m == errorMessage) { "Expected event m value to be $errorMessage, found ${e.m}" }
        }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedNID = mocks["mockedNeuroID"] as NeuroID
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper

        advancedDeviceIDManagerService.getRemoteID("testKey", "testEndpoint")

        verifyCaptureEvent(mockedNID, 1)
        verify (exactly = 1){
            mockedLogger.e(msg="Failed to get API key from NeuroID: $errorMessage")
        }
    }

    @Test
    fun testGetRemoteID_fpjs_failure() {
        val errorMessage = "FPJS Failure"
        val fullErrorMessage = "Reached maximum number of retries (${NIDAdvancedDeviceNetworkService.RETRY_COUNT}) to get Advanced Device Signal Request ID:$errorMessage"

        val mocks = buildAdvancedDeviceIDManagerService(
            networkServiceResult = Triple("", true, ""),
            fpjsResponse = Pair(null, errorMessage)
        ) { e: NIDEventModel ->
            assert(e.type == LOG) { "Expected event type to be ${LOG}, found ${e.type}" }
            assert(e.level == "error") { "Expected event level to be ${"error"}, found ${e.level}" }
            assert(e.m == fullErrorMessage)
            { "Expected event m value to be $fullErrorMessage, found ${e.m}" }
        }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedDataStore = mocks["mockedDataStore"] as NIDDataStoreManager
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper

        val job = advancedDeviceIDManagerService.getRemoteID("testKey", "testEndpoint")

        job?.invokeOnCompletion {
            verify (exactly = 1){
                mockedLogger.d(msg="Error retrieving Advanced Device Signal Request ID:$errorMessage: 1")
                mockedLogger.d(msg="Error retrieving Advanced Device Signal Request ID:$errorMessage: 2")
                mockedLogger.d(msg="Error retrieving Advanced Device Signal Request ID:$errorMessage: 3")

                mockedDataStore.saveEvent(any())
                mockedLogger.e(msg=fullErrorMessage)
            }
        }
    }

    @Test
    fun testGetRemoteID_fpjs_success(){
        val validRID = "Valid RID Key"

        val mocks = buildAdvancedDeviceIDManagerService(
            networkServiceResult = Triple("", true, ""),
            fpjsResponse = Pair(validRID, null)
        ) { e: NIDEventModel ->
            assert(e.type == ADVANCED_DEVICE_REQUEST) { "Expected event type to be ${ADVANCED_DEVICE_REQUEST}, found ${e.type}" }
            assert(e.rid == validRID) { "Expected event requestID to be ${validRID}, found ${e.rid}" }
            assert(e.c == false) { "Expected event c value to be false, found true" }
        }
        val advancedDeviceIDManagerService = mocks["advancedDeviceIDManagerService"] as AdvancedDeviceIDManagerService
        val mockedSharedPreferences = mocks["mockedSharedPreferences"] as NIDSharedPrefsDefaults
        val mockedDataStore = mocks["mockedDataStore"] as NIDDataStoreManager
        val mockedLogger = mocks["mockedLogger"] as NIDLogWrapper

        val job = advancedDeviceIDManagerService.getRemoteID("testKey", "testEndpoint")

        job?.invokeOnCompletion {
            verify (exactly = 1){
                mockedLogger.d(msg="Generating Request ID for Advanced Device Signals: $validRID")
                mockedDataStore.saveEvent(any())
                mockedLogger.d(msg="Caching Request ID: $validRID")

                mockedSharedPreferences.putString(AdvancedDeviceIDManager.NID_RID, any())
            }
        }
    }

    /*
        Mocking Functions
     */
    private fun buildAdvancedDeviceIDManagerService(
        sharedPrefGetValue:String = "{\"key\":\"testingExp\", \"exp\":0}",
        networkServiceResult:Triple<String, Boolean, String> = Triple("", false, ""),
        fpjsResponse:Pair<String?, String?> = Pair(null, null),
        saveEventTest:(e:NIDEventModel)->Unit = {},
    ):Map<String, Any>{
        val mockedNeuroID = getMockedNeuroID()
        val mockedApplication = getMockedApplication()
        val mockedSharedPreferences = getMockedSharedPrefs(AdvancedDeviceIDManager.NID_RID, sharedPrefGetValue)
        val mockedLogger = getMockedLogger()
        val mockedDataStore = getMockedDatastoreManager(saveEventTest)
        val mockedNetworkService = getMockADVNetworkService(
            networkServiceResult.first,
            networkServiceResult.second,
            networkServiceResult.third,
        )
        val mockedFPJSClient = getMockedFPJSClient(fpjsResponse.first, fpjsResponse.second)

        val advancedDeviceIDManagerService = AdvancedDeviceIDManager(
            mockedApplication,
            mockedLogger,
            mockedSharedPreferences,
            mockedNeuroID,
            mockedNetworkService,
            mockedFPJSClient
        )

        return mapOf(
            "advancedDeviceIDManagerService" to advancedDeviceIDManagerService,
            "mockedNeuroID" to mockedNeuroID,
            "mockedNetworkService" to mockedNetworkService,
            "mockedDataStore" to mockedDataStore,
            "mockedLogger" to mockedLogger,
            "mockedSharedPreferences" to mockedSharedPreferences,
            "mockedApplication" to mockedApplication,
            "mockedFPJSClient" to mockedFPJSClient
        )
    }

    private fun getMockedNeuroID(): NeuroID {
        val nidMock = mockk<NeuroID>()
        every {
            nidMock.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs

        return nidMock
    }

    private fun getMockedSharedPrefs(key:String, getValue:String = "" ):NIDSharedPrefsDefaults {
        val sharedPrefs = mockk<NIDSharedPrefsDefaults>()
        every { sharedPrefs.putString(key, any()) } just runs
        every { sharedPrefs.getString(key, any()) } returns getValue

        return sharedPrefs
    }

    private fun getMockedDatastoreManager (saveEventTest:(e:NIDEventModel)->Unit = {}): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } answers {
            saveEventTest(args[0] as NIDEventModel)
            mockk<Job>()
        }
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
        every {sensorManager.getSensorList(any())} returns listOf()
        every {sensorManager.unregisterListener(any<NIDSensorGenListener>())} just runs
        every {sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>(), any<Int>())}

        val application = mockk<Application>()
        every{application.getSystemService(any())} returns sensorManager

        val sharedPreferences = mockk<SharedPreferences>()
        every {sharedPreferences.getString(any(), any())} returns "test"
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences

        val activityManager = mockk<ActivityManager>()
        every {application.getSystemService(Context.ACTIVITY_SERVICE)} returns activityManager
        every {activityManager.getMemoryInfo(any())} just runs

        return application
    }

    private fun getMockADVNetworkService(
       key: String = "",
       success: Boolean = false,
       message:String = ""
    ): ADVNetworkService {
    val mockedADVNetworkService = mockk<ADVNetworkService>()

    every { mockedADVNetworkService.getNIDAdvancedDeviceAccessKey(any()) } returns ADVKeyFunctionResponse(
            key,
            success,
            message,
        )
        return mockedADVNetworkService
    }

    private fun getMockedFPJSClient(
        successResponse:String?,
        errorResponse:String?
    ): FingerprintJS {
        val mockedFPJSClient = mockk<FingerprintJS>()
        every { mockedFPJSClient.getVisitorId(listener = any(), errorListener = any()) }.answers {
            if (successResponse != null){
                val successListener = args[0] as (FingerprintJSProResponse) -> Unit
                val mockSuccessResponse = mockk<FingerprintJSProResponse>()
                every { mockSuccessResponse.requestId } returns successResponse
                successListener(mockSuccessResponse)
            }
            if (errorResponse != null) {
                val errorListener = args[1] as (Error) -> Unit
                val mockSuccessResponse = mockk<Error>()
                every { mockSuccessResponse.description } returns errorResponse
                errorListener(mockSuccessResponse)
            }
        }

        return mockedFPJSClient
    }


    private fun verifyCaptureEvent(nidMock: NeuroID, count:Int = 1){
        verify(exactly = count) {
            nidMock.captureEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }
}