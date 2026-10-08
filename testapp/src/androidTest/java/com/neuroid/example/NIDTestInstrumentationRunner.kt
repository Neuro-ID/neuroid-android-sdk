package com.neuroid.example
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.gson.Gson
import com.fingerprint.android.Fingerprint
import com.fingerprint.android.FingerprintResponse
import com.neuroid.tracker.NeuroID
import io.mockk.every
import io.mockk.mockk
import com.fingerprint.android.Error

class NIDTestInstrumentationRunner  : AndroidJUnitRunner() {
    private val gson = Gson()

    private fun getMockedFPJSClient(
        successResponse: String?,
        errorResponse: String?,
        sealedResult: String?
    ): Fingerprint {
        val mockedFPJSClient = mockk<Fingerprint>()
        every { mockedFPJSClient.getVisitorId(tags = ofType<Map<String, Any>>(), listener = any(), errorListener = any()) }.answers {
            if (successResponse != null) {
                val successListener = args[1] as (FingerprintResponse) -> Unit
                val mockSuccessResponse = mockk<FingerprintResponse>()
                every {mockSuccessResponse.sealedResult} returns sealedResult
                every {mockSuccessResponse.eventId} returns successResponse
                successListener(mockSuccessResponse)
            }
            if (errorResponse != null) {
                val errorListener = args[2] as (Error) -> Unit
                val mockSuccessResponse = mockk<Error>()
                every { mockSuccessResponse.description } returns errorResponse
                errorListener(mockSuccessResponse)
            }
        }

        return mockedFPJSClient
    }
    override fun onCreate(arguments: Bundle) {
        NIDTestInstrumentation.clearRecorders()
        NeuroID.fpjsClientOverride = getMockedFPJSClient("test-visitor-id", null, null)
        NeuroID.outboundPayloadObserver = { payload ->
            runCatching {
                gson.fromJson(payload, EventModel::class.java)
            }.getOrNull()?.let { eventModel ->
                NIDTestInstrumentation.attemptedRecorder.addEvent(eventModel)
            }
        }
        val thread = Thread { NIDTestInstrumentation.start() }
        thread.start()
        thread.join()
        super.onCreate(arguments)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        NeuroID.clearTestObservers()
        NIDTestInstrumentation.shutdown()
        super.finish(resultCode, results)
    }
}