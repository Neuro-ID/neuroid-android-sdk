package com.neuroid.tracker

import com.neuroid.tracker.service.NIDResponseCallBack
import com.neuroid.tracker.service.NIDApiService
import com.neuroid.tracker.service.NIDEventSender
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class NeuroIDSendEventTest {
    @Test
    fun `successful send request test`() = runBlocking {
        val apiService = mockk<NIDApiService>()
        val call = mockk<Call<ResponseBody>>()
        val retryClone = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>()
        every {response.isSuccessful} returns true
        every {response.code()} returns 200
        every {response.message()} returns "should not happen"
        every {retryClone.execute()} returns response
        every {call.clone()} returns retryClone
        every {apiService.sendEvents(any(), any())} returns call
        val callback = mockk<NIDResponseCallBack>()
        every { callback.onSuccess(any()) } just Runs
        every { callback.onFailure(any(), any(), any()) } just Runs
        val events = "{\"siteId\":\"doom_109\",\"userId\":\"1693537992533\",\"clientId\"}"
        val eventSender = NIDEventSender(apiService)
        eventSender.sendTrackerData(events, "test", callback)
        verify {
            callback.onSuccess(200)
        }
    }

    @Test
    fun `failed send request 3 times test`() = runBlocking {
        val apiService = mockk<NIDApiService>()
        val call = mockk<Call<ResponseBody>>()
        val retryClone = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>()
        val responseBody = mockk<ResponseBody>()
        every {responseBody.close()} just Runs
        every {response.isSuccessful} returns false
        every {response.code()} returns 400
        every {response.message()} returns "your request is junk, fix it!"
        every {response.body()} returns responseBody
        every {retryClone.execute()} returns response
        every {call.clone()} returns retryClone
        every {apiService.sendEvents(any(), any())} returns call
        val callback = mockk<NIDResponseCallBack>()
        every { callback.onSuccess(any()) } just Runs
        every { callback.onFailure(any(), any(), any()) } just Runs
        val events = "{\"siteId\":\"guns_452\",\"userId\":\"1693537992533\"}"
        val eventSender = NIDEventSender(apiService)
        eventSender.sendTrackerData(events, "test", callback)
        verify(exactly = 3) {
            callback.onFailure(400, "your request is junk, fix it!", any())
        }
    }

    @Test
    fun `failed exception send request test`() = runBlocking {
        val apiService = mockk<NIDApiService>()
        val call = mockk<Call<ResponseBody>>()
        val retryClone = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>()
        every {response.isSuccessful} returns false
        every {response.code()} returns 400
        every {response.message()} returns "your request is junk, fix it!"
        every {retryClone.execute()} throws Exception("we blew it, sorry")
        every {call.clone()} returns retryClone
        every {apiService.sendEvents(any(), any())} returns call
        val callback = mockk<NIDResponseCallBack>()
        every { callback.onSuccess(any()) } just Runs
        every { callback.onFailure(any(), any(), any()) } just Runs
        val events = "{\"siteId\":\"guns_452\",\"userId\":\"1693537992533\"}"
        val eventSender = NIDEventSender(apiService)
        eventSender.sendTrackerData(events, "test", callback)
        verify {
            callback.onFailure(-1, "we blew it, sorry", any())
        }
    }

}