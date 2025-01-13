package com.neuroid.tracker

import com.neuroid.tracker.models.ADVKeyNetworkResponse
import com.neuroid.tracker.service.NIDAdvancedDeviceApiService
import com.neuroid.tracker.service.NIDAdvancedDeviceNetworkService
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class NIDAdvancedDeviceNetworkServiceUnitTests {
    //    getNIDAdvancedDeviceAccessKey
    @Test
    fun testGetNIDAdvancedDeviceAccessKey_failed_retries() {
        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    getMockedOkHTTPCall(Triple(400, null, "Error")),
                ),
                getMockedLogger(),
            )

        val response = advancedDeviceNetworkService.getNIDAdvancedDeviceAccessKey("testKey", "", "")

        assert(!response.success) { "Expected response.success should be false" }
        assert(response.message == "Error") { "Expected response.message should be Errors, found ${response.message}" }
        assert(response.key == "") { "Expected response.key should be \"\", found ${response.key}" }
    }

    @Test
    fun testGetNIDAdvancedDeviceAccessKey_failed_exception() {
        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    getMockedOkHTTPCall(
                        Triple(400, null, ""),
                        "Exception Message",
                    ),
                ),
                getMockedLogger(),
            )

        val response = advancedDeviceNetworkService.getNIDAdvancedDeviceAccessKey("testKey", "", "")

        assert(!response.success) { "Expected response.success should be false" }
        assert(
            response.message == "Exception Message",
        ) { "Expected response.message should be Exception Message, found ${response.message}" }
        assert(response.key == "") { "Expected response.key should be \"\", found ${response.key}" }
    }

    @Test
    fun testGetNIDAdvancedDeviceAccessKey_failed_status_bad() {
        val errorMessage = "advanced signal not available: status BAD"
        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    getMockedOkHTTPCall(
                        Triple(
                            200,
                            ADVKeyNetworkResponse("BAD", ""),
                            "",
                        ),
                    ),
                ),
                getMockedLogger(),
            )

        val response = advancedDeviceNetworkService.getNIDAdvancedDeviceAccessKey("testKey", "", "")

        assert(!response.success) { "Expected response.success should be false" }
        assert(response.message == errorMessage) { "Expected response.message should be \"$errorMessage\", found \"${response.message}\"" }
        assert(response.key == "") { "Expected response.key should be \"\", found ${response.key}" }
    }

    @Test
    fun testGetNIDAdvancedDeviceAccessKey_success() {
        val key = "VALIDKEY"
        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    getMockedOkHTTPCall(
                        Triple(
                            200,
                            ADVKeyNetworkResponse("OK", key),
                            "",
                        ),
                    ),
                ),
                getMockedLogger(),
                getMockedBase64Decoder(key),
            )

        val response = advancedDeviceNetworkService.getNIDAdvancedDeviceAccessKey("testKey", "", "")

        assert(response.success) { "Expected response.success should be true" }
        assert(response.message == null) { "Expected response.message should be \"\", found \"${response.message}\"" }
        assert(response.key == key) { "Expected response.key should be \"$key\", found ${response.key}" }
    }

    //    retryRequests
    @Test
    fun testRetryRequests_failure_max_retry() {
        val errorMessage = "Error Message"
        val call =
            getMockedOkHTTPCall(
                Triple(400, null, errorMessage),
            )
        val mockedLogger = getMockedLogger()

        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    call,
                ),
                mockedLogger,
            )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(!response.success) { "Expected response.success should be false" }
        assert(response.message == errorMessage) { "Expected response.message should be \"$errorMessage\", found \"${response.message}\"" }
        assert(response.key == "") { "Expected response.key should be \"\", found ${response.key}" }

        verify(exactly = 2) {
            mockedLogger.d(
                tag = "NeuroID ADV",
                msg = "Failed to get API key from NeuroID: $errorMessage - Code: ${400}. Retrying: ${true}",
            )
        }

        verify(exactly = 1) {
            mockedLogger.d(
                tag = "NeuroID ADV",
                msg = "Failed to get API key from NeuroID: $errorMessage - Code: ${400}. Retrying: ${false}",
            )
        }
    }

    @Test
    fun testRetryRequests_failure_exception() {
        val exceptionMessage = "Exception Message"
        val call =
            getMockedOkHTTPCall(
                Triple(400, null, ""),
                exceptionMessage,
            )
        val mockedLogger = getMockedLogger()

        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(
                    call,
                ),
                mockedLogger,
            )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(!response.success) { "Expected response.success should be false" }
        assert(
            response.message == exceptionMessage,
        ) { "Expected response.message should be \"$exceptionMessage\", found \"${response.message}\"" }
        assert(response.key == "") { "Expected response.key should be \"\", found ${response.key}" }
    }

    @Test
    fun testRetryRequests_success() {
        val key = "VALIDKEY"
        val call =
            getMockedOkHTTPCall(
                Triple(
                    200,
                    ADVKeyNetworkResponse("OK", key),
                    "",
                ),
            )

        val advancedDeviceNetworkService =
            NIDAdvancedDeviceNetworkService(
                getMockedAPIService(call),
                getMockedLogger(),
                getMockedBase64Decoder(key),
            )

        val response = advancedDeviceNetworkService.retryRequests(call)

        assert(response.success) { "Expected response.success should be true" }
        assert(response.message == null) { "Expected response.message should be \"\", found \"${response.message}\"" }
        assert(response.key == key) { "Expected response.key should be \"$key\", found ${response.key}" }
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

    private fun getMockedBase64Decoder(result: String): Base64Decoder {
        val b64 = mockk<Base64Decoder>()
        every { b64.decodeBase64(any()) } returns result
        return b64
    }

    private fun getMockedAPIService(call: Call<ADVKeyNetworkResponse>): NIDAdvancedDeviceApiService {
        val apiService = mockk<NIDAdvancedDeviceApiService>()
        every { apiService.getNIDAdvancedDeviceAccessKey(any(), any(), any()) } returns call

        return apiService
    }

    private fun getMockedOkHTTPCall(
        responseItems: Triple<Int, ADVKeyNetworkResponse?, String> = Triple(1, null, ""), // code, body, message
        retryErrorMessage: String? = null,
    ): Call<ADVKeyNetworkResponse> {
        var responseBody: Any? = ADVKeyNetworkResponse("", "")
        if (responseItems.second != null) {
            responseBody = responseItems.second!!
        }

        val response = mockk<Response<ADVKeyNetworkResponse>>()
        every { response.code() } returns responseItems.first
        every { response.message() } returns responseItems.third
        every { response.body() } returns responseBody as ADVKeyNetworkResponse

        val retryClone = mockk<Call<ADVKeyNetworkResponse>>()
        if (retryErrorMessage != null) {
            every { retryClone.execute() } throws Exception(retryErrorMessage)
        } else {
            every { retryClone.execute() } returns response
        }

        val call = mockk<Call<ADVKeyNetworkResponse>>()
        every { call.clone() } returns retryClone

        return call
    }
}
