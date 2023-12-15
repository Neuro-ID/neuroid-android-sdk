package com.neuroid.tracker

import android.app.Application
import android.content.SharedPreferences
import android.hardware.SensorManager
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.service.NIDApiService
import com.neuroid.tracker.service.NIDEventSender
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDResponseCallBack
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class NIDJobServiceManagerTest {
    @Test
    fun testStartjob() {
        injectMockedApplication()
        assertEquals(NIDJobServiceManager.isSetup, true)
        assertEquals(NIDJobServiceManager.clientKey, "clientKey")
        assertEquals(NIDJobServiceManager.endpoint, "endpoint")
        assertNotNull(NIDJobServiceManager.jobCaptureEvents)
    }

    @Test
    fun testStopjob() {
        injectMockedApplication()
        NIDJobServiceManager.stopJob()
        assertEquals(NIDJobServiceManager.jobCaptureEvents, null)
        assertEquals(NIDJobServiceManager.isStopped(), true)
    }

    @Test
    fun testRestart() {
        NIDJobServiceManager.restart()
        assertNotNull(NIDJobServiceManager.jobCaptureEvents)
    }

    @Test
    fun testGetApiService() {
        NIDJobServiceManager.endpoint = "https://test.com"
        assertNotNull(NIDJobServiceManager.getServiceAPI())
    }

    @Test
    fun testSendEventsNowSuccess() {
        runTest {
            injectMockedApplication()

            val dataStoreManager = getMockedDatastoreManager()
            val logger = getMockedLogger()

            //prepare the sender response with a 200 OK, this will trigger the onSuccess() callback
            val eventSender = getMockEventSender(
                true,
                200,
                "should not happen")

            // test the thing
            NIDJobServiceManager.sendEventsNow(
                logger,
                forceSendEvents = true,
                eventSender = eventSender,
                dataStoreManager = dataStoreManager
            )
            verify(exactly = 1) {logger.d(msg=" network success, sendEventsNow() success userActive: true")}
        }
    }


    @Test
    fun testSendEventsNowError() {
        runTest {
            injectMockedApplication()

            val dataStoreManager = getMockedDatastoreManager()
            val logger = getMockedLogger()

            //prepare the sender response with a 400 error, this will trigger the onError() callback
            val eventSender = getMockEventSender(
                false,
                400,
                "your request is junk, fix it!")

            // test the thing
            NIDJobServiceManager.sendEventsNow(
                logger, forceSendEvents = true,
                eventSender = eventSender,
                dataStoreManager = dataStoreManager
            )
            verify (exactly = 2) {logger.e(msg="network failure, sendEventsNow() failed userActive: true your request is junk, fix it!")}
            verify (exactly = 1) {logger.e(msg="network failure, sendEventsNow() failed userActive: false your request is junk, fix it!")}
        }
    }

    private fun getMockedDatastoreManager (): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        val events = "{\"siteId\":\"guns_452\",\"userId\":\"1693537992533\"}"
        coEvery {dataStoreManager.getAllEvents()} returns setOf(events)
        return dataStoreManager
    }

    private fun getMockedLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        return logger
    }

    private fun injectMockedApplication() {
        val sensorManager = mockk<SensorManager>()
        every {sensorManager.getSensorList(any())} returns listOf()
        every {sensorManager.unregisterListener(any<NIDSensorGenListener>())} just runs
        val application = mockk<Application>()
        every{application.getSystemService(any())} returns sensorManager
        val sharedPreferences = mockk<SharedPreferences>()
        every {sharedPreferences.getString(any(), any())} returns "test"
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences
        NIDJobServiceManager.startJob(application, "clientKey", "endpoint")
    }

    private fun getMockEventSender(isSuccess: Boolean,
                                   respCode: Int,
                                   respMessage: String): NIDEventSender {
        val apiService = mockk<NIDApiService>()
        val call = mockk<Call<ResponseBody>>()
        val retryClone = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>()
        val responseBody = mockk<ResponseBody>()
        every {responseBody.close()} just Runs
        every {response.isSuccessful} returns isSuccess
        every {response.code()} returns respCode
        every {response.message()} returns respMessage
        every {response.body()} returns responseBody
        every {retryClone.execute()} returns response
        every {call.clone()} returns retryClone
        every {apiService.sendEvents(any(), any())} returns call
        val callback = mockk<NIDResponseCallBack>()
        every { callback.onSuccess(any()) } just Runs
        every { callback.onFailure(any(), any(), any()) } just Runs
        return NIDEventSender(apiService)
    }


}