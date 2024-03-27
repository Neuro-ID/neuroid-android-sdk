package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.models.NIDEventModel
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class NeuroIDSendEventTest {
    //    sendEvents
    @Test
    fun testSendEvents_success() = runBlocking {

        val mockedAPIService = getMockedAPIService(
            true,
            200,
            "should not happen",

            )
        val callback = getMockedNIDCallback()

        val events = listOf(NIDEventModel(type = "TEST_EVENT", ts =1))
        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.sendEvents("test_key", events, callback)
        verify {
            callback.onSuccess(200, null)
        }
    }

    @Test
    fun testSendEvents_fail_3_times() = runBlocking {

        val mockedAPIService = getMockedAPIService(
            false,
            400,
            "your request is junk, fix it!",

        )
        val callback = getMockedNIDCallback()

        val events = listOf(NIDEventModel(type = "TEST_EVENT", ts =1))
        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.sendEvents("test_key", events, callback)

        verify(exactly = 3) {
            callback.onFailure(400, "your request is junk, fix it!", any())
        }
    }

    @Test
    fun testSendEvents_fail_exception() = runBlocking {
        val mockedAPIService = getMockedAPIService(
            false,
            400,
            "your request is junk, fix it!",
            "we blew it, sorry"
        )
        val callback = getMockedNIDCallback()

        val events = listOf(NIDEventModel(type = "TEST_EVENT", ts =1))
        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.sendEvents("test_key", events, callback)

        verify {
            callback.onFailure(-1, "we blew it, sorry", any())
        }
    }


    //    getRequestPayloadJSON
    @Test
    fun testgetRequestPayloadJSON(){
        val mockedAPIService = getMockedAPIService(
            false,
            400,
            "",
        )

        val events = listOf(NIDEventModel(type = "TEST_EVENT", ts =1))
        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        val payload = eventSender.getRequestPayloadJSON(
            events
        )

        // Complete payload
        // {"siteId":"","clientId":"test","pageTag":"","pageId":"mobile","tabId":"mobile","responseId":"599ad307-18ed-45f4-89de-b633861eaac9","url":"android://","jsVersion":"5.0.0","sdkVersion":"5.android-null3.1.0","environment":"","jsonEvents":[{"type":"TEST_EVENT","ts":1}]}
         assert(payload.contains("\"type\":\"TEST_EVENT\"")) {
            "Payload structure is incorrect"
        }
    }

    //    retryRequests
    @Test
    fun testRetryRequests_success(){
        val mockedAPIService = getMockedAPIService(
            true,
            200,
            "",
        )
        val callback = getMockedNIDCallback()
        val mockedCall = getMockedOkHTTPCall(
            true,
            200,
            "Success",
        )

        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.retryRequests(mockedCall, callback)

        verify {
            callback.onSuccess(200, null)
        }

    }

    @Test
    fun testRetryRequests_failed(){
        val mockedAPIService = getMockedAPIService(
            true,
            200,
            "",
        )
        val callback = getMockedNIDCallback()
        val mockedCall = getMockedOkHTTPCall(
            false,
            400,
            "Errored out",
        )

        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.retryRequests(mockedCall, callback)

        verify(exactly = 3) {
            callback.onFailure(400, message = "Errored out", isRetry = any())
        }
    }

    @Test
    fun testRetryRequests_exception(){
        val mockedAPIService = getMockedAPIService(
            true,
            200,
            "",
        )
        val callback = getMockedNIDCallback()
        val mockedCall = getMockedOkHTTPCall(
            false,
            400,
            "Errored out",
            "we blew it, sorry"
        )

        val eventSender = NIDEventSender(mockedAPIService, getMockedApplication())

        eventSender.retryRequests(mockedCall, callback)

        verify {
            callback.onFailure(-1, "we blew it, sorry", false)
        }
    }

    /*
    Helper Mocking Functions
     */
    private fun getMockedApplication():Application {
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

    private fun getMockedAPIService(
        isSuccessful:Boolean,
        responseCode:Int,
        responseMessage:String,
        retryErrorMessage:String? = null
    ):NIDApiService{
        val call = getMockedOkHTTPCall(
            isSuccessful,
            responseCode,
            responseMessage,
            retryErrorMessage
        )

        val apiService = mockk<NIDApiService>()
        every {apiService.sendEvents(any(), any())} returns call

        return apiService
    }

    private fun getMockedOkHTTPCall(
        isSuccessful:Boolean,
        responseCode:Int,
        responseMessage:String,
        retryErrorMessage:String? = null
    ):Call<ResponseBody>{
        val responseBody = mockk<ResponseBody>()
        every {responseBody.close()} just Runs

        val response = mockk<Response<ResponseBody>>()
        every {response.isSuccessful} returns isSuccessful
        every {response.code()} returns responseCode
        every {response.message()} returns responseMessage
        every {response.body()} returns responseBody

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

    private fun getMockedNIDCallback():NIDResponseCallBack{
        val callback = mockk<NIDResponseCallBack>()
        every { callback.onSuccess(any(), null) } just Runs
        every { callback.onFailure(any(), any(), any()) } just Runs

        return callback
    }

}