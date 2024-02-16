package com.neuroid.tracker

import com.neuroid.tracker.service.NIDAdvancedDeviceApiService
import com.neuroid.tracker.service.NIDAdvancedDeviceNetworkService
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import okhttp3.ResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class NIDAdvancedDeviceNetworkServiceUnitTests {

    //    getADVKey
    @Test
    fun testGetADVKey_failed_retries(){
        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                getMockedOkHTTPCall(Triple(400, "", "Error"))
            ),
            getMockedLogger()
        )

        val response = advancedDeviceNetworkService.getADVKey("testKey")

        assert(!response.success) {"Expected response.success should be false"}
        assert(response.message == "Error") {"Expected response.message should be Errors, found ${response.message}"}
        assert(response.key == "") {"Expected response.key should be \"\", found ${response.key}"}
    }

    @Test
    fun testGetADVKey_failed_exception(){
        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                getMockedOkHTTPCall(Triple(400, "", ""),
                "Exception Message")
            ),
            getMockedLogger()
        )

        val response = advancedDeviceNetworkService.getADVKey("testKey")

        assert(!response.success) {"Expected response.success should be false"}
        assert(response.message == "Exception Message") {"Expected response.message should be Exception Message, found ${response.message}"}
        assert(response.key == "") {"Expected response.key should be \"\", found ${response.key}"}
    }

    @Test
    fun testGetADVKey_failed_status_bad(){
        val errorMessage = "advanced signal not available: status BAD"
        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                getMockedOkHTTPCall(Triple(200, "{\"status\": \"BAD\", \"key\":\"\" }", ""))
            ),
            getMockedLogger()
        )

        val response = advancedDeviceNetworkService.getADVKey("testKey")

        assert(!response.success) {"Expected response.success should be false"}
        assert(response.message == errorMessage) {"Expected response.message should be \"$errorMessage\", found \"${response.message}\""}
        assert(response.key == "") {"Expected response.key should be \"\", found ${response.key}"}
    }

    @Test
    fun testGetADVKey_success(){
        val key = "VALIDKEY"
        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                getMockedOkHTTPCall(Triple(200, "{\"status\": \"OK\", \"key\":\"$key\" }", ""))
            ),
            getMockedLogger()
        )

        val response = advancedDeviceNetworkService.getADVKey("testKey")

        assert(response.success) {"Expected response.success should be true"}
        assert(response.message == null) {"Expected response.message should be \"\", found \"${response.message}\""}
        assert(response.key == key) {"Expected response.key should be \"$key\", found ${response.key}"}
    }

    //    retryRequests
    @Test
    fun testRetryRequests_failure_max_retry(){
        val errorMessage = "Error Message"
        val call = getMockedOkHTTPCall(
            Triple(400, "", errorMessage)
        )
        val mockedLogger = getMockedLogger()

        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                call
            ),
            mockedLogger
        )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(!response.success) {"Expected response.success should be false"}
        assert(response.message == errorMessage) {"Expected response.message should be \"$errorMessage\", found \"${response.message}\""}
        assert(response.key == "") {"Expected response.key should be \"\", found ${response.key}"}

        verify (exactly = 2) {
            mockedLogger.d(
                tag = "NeuroID ADV",
                msg = "Failed to get API key from NeuroID: $errorMessage - Code: ${400}. Retrying: ${true}"
            )
        }

        verify (exactly = 1) {
            mockedLogger.d(
                tag = "NeuroID ADV",
                msg = "Failed to get API key from NeuroID: $errorMessage - Code: ${400}. Retrying: ${false}"
            )
        }
    }

    @Test
    fun testRetryRequests_failure_exception(){
        val exceptionMessage = "Exception Message"
        val call = getMockedOkHTTPCall(
            Triple(400, "", ""),
            exceptionMessage
        )
        val mockedLogger = getMockedLogger()

        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                call
            ),
            mockedLogger
        )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(!response.success) {"Expected response.success should be false"}
        assert(response.message == exceptionMessage) {"Expected response.message should be \"$exceptionMessage\", found \"${response.message}\""}
        assert(response.key == "") {"Expected response.key should be \"\", found ${response.key}"}
    }

    @Test
    fun testRetryRequests_success(){
        val key = "VALIDKEY"
        val call = getMockedOkHTTPCall(
            Triple(200, "{\"status\": \"OK\", \"key\":\"$key\" }", "")
        )

        val advancedDeviceNetworkService = NIDAdvancedDeviceNetworkService(
            getMockedAPIService(
                call
            ),
            getMockedLogger()
        )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(response.success) {"Expected response.success should be true"}
        assert(response.message == null) {"Expected response.message should be \"\", found \"${response.message}\""}
        assert(response.key == key) {"Expected response.key should be \"$key\", found ${response.key}"}
    }

    /*
    Mocking Functions
     */

    private fun getMockedLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        return logger
    }

    private fun getMockedAPIService(
        call:Call<ResponseBody>
    ): NIDAdvancedDeviceApiService {

        val apiService = mockk<NIDAdvancedDeviceApiService>()
        every {apiService.getADVKey(any())} returns call

        return apiService
    }

    private fun getMockedOkHTTPCall(
        responseItems:Triple<Int, String?, String> = Triple(1, "", ""), // code, body, message
        retryErrorMessage:String? = null
    ):Call<ResponseBody>{

        var responseBody:Any? = null
        if (responseItems.second != null ){
            responseBody = mockk<ResponseBody>()
            every {responseBody.close()} just Runs
            every { responseBody.string() } returns responseItems.second!!
        }

        val response = mockk<Response<ResponseBody>>()
        every {response.code()} returns responseItems.first
        every {response.message()} returns responseItems.third
        every {response.body()} returns responseBody as ResponseBody

        val retryClone = mockk<Call<ResponseBody>>()
        if(retryErrorMessage != null) {
            every {retryClone.execute()} throws Exception(retryErrorMessage)
        } else {
            every {retryClone.execute()} returns response
        }

        val call = mockk<Call<ResponseBody>>()
        every {call.clone()} returns retryClone

        return call
    }

}