package com.neuroid.tracker

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDApiService
import com.neuroid.tracker.service.NIDEventSender
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDResponseCallBack
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.Constants
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
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.second
        val nidJobServiceManager = mockedSetup.first

        assertEquals(nidJobServiceManager.jobCaptureEvents, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isSetup, false)
        assertEquals(nidJobServiceManager.clientKey, "")


        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey"
        )

        assertEquals(nidJobServiceManager.isSetup, true)
        assertEquals(nidJobServiceManager.clientKey, "clientKey")
        assertEquals(nidJobServiceManager.endpoint, Constants.productionEndpoint.displayName)
        assertNotNull(nidJobServiceManager.jobCaptureEvents)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
    }

    @Test
    fun testStopjob() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.second
        val nidJobServiceManager = mockedSetup.first

        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey"
        )
        assertNotNull(nidJobServiceManager.jobCaptureEvents)

        nidJobServiceManager.stopJob()
        assertEquals(nidJobServiceManager.jobCaptureEvents, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)
    }

    @Test
    fun testRestart() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val nidJobServiceManager = mockedSetup.first

        assertEquals(nidJobServiceManager.jobCaptureEvents, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)

        nidJobServiceManager.restart()
        assertNotNull(nidJobServiceManager.jobCaptureEvents)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
        assertEquals(nidJobServiceManager.isStopped(), false)
    }

    @Test
    fun testGetApiService() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val nidJobServiceManager = mockedSetup.first

        nidJobServiceManager.endpoint = Constants.productionEndpoint.displayName
        assertNotNull(nidJobServiceManager.getServiceAPI())
    }

    @Test
    fun testSendEventsNowSuccess() {
        runTest {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val logger = mockedSetup.third
            val mockedApplication = mockedSetup.second
            val nidJobServiceManager = mockedSetup.first
            nidJobServiceManager.startJob(
                mockedApplication,
                "clientKey"
            )

            //prepare the sender response with a 200 OK, this will trigger the onSuccess() callback
            val eventSender = getMockEventSender(
                true,
                200,
                "should not happen")

            // test the thing
            nidJobServiceManager.sendEventsNow(
                forceSendEvents = true,
                eventSender = eventSender,
            )
            verify(exactly = 1) {logger.d(msg=" network success, sendEventsNow() success userActive: true")}
        }
    }


    @Test
    fun testSendEventsNowError() {
        runTest {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val logger = mockedSetup.third
            val mockedApplication = mockedSetup.second
            val nidJobServiceManager = mockedSetup.first
            nidJobServiceManager.startJob(
                mockedApplication,
                "clientKey"
            )

            //prepare the sender response with a 400 error, this will trigger the onError() callback
            val eventSender = getMockEventSender(
                false,
                400,
                "your request is junk, fix it!")

            // test the thing
            nidJobServiceManager.sendEventsNow(
                forceSendEvents = true,
                eventSender = eventSender,
            )
            verify (exactly = 2) {logger.e(msg="network failure, sendEventsNow() failed retrylimitHit: false your request is junk, fix it!")}
            verify (exactly = 1) {logger.e(msg="network failure, sendEventsNow() failed retrylimitHit: true your request is junk, fix it!")}
        }
    }

    private fun setupNIDJobServiceManagerMocks():Triple<NIDJobServiceManager, Application, NIDLogWrapper>{
        val mockedApplication = getMockedApplication()
        val logger = getMockedLogger()
        val nidJobServiceManager = NIDJobServiceManager(
            logger,
            getMockedDatastoreManager()
        )

        return Triple(nidJobServiceManager, mockedApplication, logger)
    }

    private fun getMockedDatastoreManager (): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        val event = NIDEventModel(type = "TEST_EVENT", ts=1)
        coEvery {dataStoreManager.getAllEvents()} returns listOf(event)
        return dataStoreManager
    }

    private fun getMockedLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs
        return logger
    }

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