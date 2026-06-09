package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.events.CADENCE_READING_ACCEL
import com.neuroid.tracker.events.ERROR
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.LOW_MEMORY
import com.neuroid.tracker.getMockedConfigService
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.time.Duration

class NIDJobServiceManagerTest {
    @Test
    fun testStartjob() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.mockedApplication
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isSetup, false)
        assertEquals(nidJobServiceManager.clientKey, "")

        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey",
        )

        assertEquals(nidJobServiceManager.isSetup, true)
        assertEquals(nidJobServiceManager.clientKey, "clientKey")
        assertNotNull(nidJobServiceManager.sendCadenceJob)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
    }

    @Test
    fun testStopjob() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val mockedApplication = mockedSetup.mockedApplication
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        nidJobServiceManager.startJob(
            mockedApplication,
            "clientKey",
        )
        assertNotNull(nidJobServiceManager.sendCadenceJob)

        nidJobServiceManager.stopJob()
        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)
    }

    @Test
    fun testRestart() {
        val mockedSetup = setupNIDJobServiceManagerMocks()
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        assertEquals(nidJobServiceManager.sendCadenceJob, null)
        assertEquals(nidJobServiceManager.gyroCadenceJob, null)
        assertEquals(nidJobServiceManager.isStopped(), true)

        nidJobServiceManager.restart()
        assertNotNull(nidJobServiceManager.sendCadenceJob)
        assertNotNull(nidJobServiceManager.gyroCadenceJob)
        assertEquals(nidJobServiceManager.isStopped(), false)
    }

    @Test
    fun testSendEvents() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val mockedApplication = mockedSetup.mockedApplication
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            nidJobServiceManager.startJob(
                mockedApplication,
                "clientKey",
            )

            // test the thing
            nidJobServiceManager.sendEvents(
                forceSendEvents = true,
            )

            verify {
                mockedSetup.mockedEventSender.sendEvents(
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @After
    fun tearDown() {
        // Several tests flip the global SDK started flag to exercise the gyro cadence loop.
        // Always reset it so we don't leak state into other test classes.
        NeuroID._isSDKStarted = false
    }

    // ---------------------------------------------------------------------------------------------
    // checkMemoryLevel() - exercised through the public sendEvents() -> getEventsToSend() path
    // ---------------------------------------------------------------------------------------------

    @Test
    fun test_checkMemoryLevel_whenLowMemory_sendsLowMemoryEvent() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks(lowMemory = true)
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            val capturedEvents = slot<List<NIDEventModel>>()
            every {
                mockedSetup.mockedEventSender.sendEvents(any(), capture(capturedEvents), any())
            } just runs

            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            }

            // When the device reports low memory, a single LOW_MEMORY event is sent instead of
            // the queued events from the data store.
            assertEquals(1, capturedEvents.captured.size)
            assertEquals(LOW_MEMORY, capturedEvents.captured.first().type)
        }

    @Test
    fun test_checkMemoryLevel_whenNotLowMemory_sendsDataStoreEvents() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks(lowMemory = false)
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            val capturedEvents = slot<List<NIDEventModel>>()
            every {
                mockedSetup.mockedEventSender.sendEvents(any(), capture(capturedEvents), any())
            } just runs

            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            }

            // With normal memory the events come straight from the data store.
            assertEquals(1, capturedEvents.captured.size)
            assertEquals("TEST_EVENT", capturedEvents.captured.first().type)
        }

    // ---------------------------------------------------------------------------------------------
    // createGyroCadenceServer() - started by startJob() / restart()
    // ---------------------------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test_createGyroCadenceServer_capturesAccelEventOnCadence() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = UnconfinedTestDispatcher(scheduler)

        NeuroID._isSDKStarted = true
        val mockedSetup =
            setupNIDJobServiceManagerMocks(
                dispatcher = dispatcher,
                gyroAccelCadence = true,
                gyroAccelCadenceTime = 200L,
            )
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")

        // Advance exactly one cadence interval -> exactly one accel cadence event is captured.
        scheduler.advanceTimeBy(200L)
        scheduler.runCurrent()

        verifyCaptureEvent(mockedSetup.mockedNeuroID, CADENCE_READING_ACCEL, 1)

        // Terminate the loops so no further events are produced.
        NeuroID._isSDKStarted = false
        nidJobServiceManager.stopJob()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test_createGyroCadenceServer_doesNotCaptureWhenCadenceDisabled() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = UnconfinedTestDispatcher(scheduler)

        NeuroID._isSDKStarted = true
        val mockedSetup =
            setupNIDJobServiceManagerMocks(
                dispatcher = dispatcher,
                gyroAccelCadence = false,
                gyroAccelCadenceTime = 200L,
            )
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")

        scheduler.advanceTimeBy(1000L)
        scheduler.runCurrent()

        // The cadence flag is off, so no accel cadence event should ever be captured.
        verifyCaptureEvent(mockedSetup.mockedNeuroID, CADENCE_READING_ACCEL, 0)

        nidJobServiceManager.stopJob()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test_createGyroCadenceServer_stopsWhenJobStopped() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = UnconfinedTestDispatcher(scheduler)

        NeuroID._isSDKStarted = true
        val mockedSetup =
            setupNIDJobServiceManagerMocks(
                dispatcher = dispatcher,
                gyroAccelCadence = true,
                gyroAccelCadenceTime = 200L,
            )
        val nidJobServiceManager = mockedSetup.nidJobServiceManager

        nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")

        // First cadence interval captures one event.
        scheduler.advanceTimeBy(200L)
        scheduler.runCurrent()
        verifyCaptureEvent(mockedSetup.mockedNeuroID, CADENCE_READING_ACCEL, 1)

        // Stopping the job cancels the cadence coroutine, so no further events are captured even
        // after additional time elapses.
        nidJobServiceManager.stopJob()
        scheduler.advanceTimeBy(1000L)
        scheduler.runCurrent()
        verifyCaptureEvent(mockedSetup.mockedNeuroID, CADENCE_READING_ACCEL, 1)

        NeuroID._isSDKStarted = false
    }

    // ---------------------------------------------------------------------------------------------
    // sendEventsNow() - exercised through the public sendEvents() channel notification
    // ---------------------------------------------------------------------------------------------

    @Test
    fun test_sendEventsNow_onSuccess_incrementsPacketNumber() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            every {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            } answers {
                val callback = thirdArg<NIDResponseCallBack<Any>>()
                callback.onSuccess(200, "success")
            }

            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify { mockedSetup.mockedNeuroID.incrementPacketNumber() }
            verify { mockedSetup.mockedLogger.d(any(), any()) }
        }

    @Test
    fun test_sendEventsNow_onFailure_capturesErrorLog() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            every {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            } answers {
                val callback = thirdArg<NIDResponseCallBack<Any>>()
                callback.onFailure(400, "bad request", false)
            }

            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify { mockedSetup.mockedLogger.e(any(), any()) }
            verifyCaptureEvent(mockedSetup.mockedNeuroID, LOG, 1, level = ERROR)
        }

    @Test
    fun test_sendEventsNow_whenSendDisabled_doesNotSend() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            nidJobServiceManager.isSendEventsNowEnabled = false
            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify(exactly = 0) {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            }
        }

    @Test
    fun test_sendEventsNow_whenApplicationNull_setsUserInactive() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            // Never call startJob(), so the application reference stays null.
            assertEquals(true, nidJobServiceManager.userActive)

            nidJobServiceManager.sendEvents(forceSendEvents = true)

            verify(exactly = 0) {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            }
            assertEquals(false, nidJobServiceManager.userActive)
        }

    // ---------------------------------------------------------------------------------------------
    // setTestEventSender()
    // ---------------------------------------------------------------------------------------------

    @Test
    fun test_setTestEventSender_replacesEventSenderUsedToSend() =
        runTest(timeout = Duration.parse("120s")) {
            val mockedSetup = setupNIDJobServiceManagerMocks()
            val nidJobServiceManager = mockedSetup.nidJobServiceManager

            val replacementEventSender = mockk<NIDSendingService>()
            every {
                replacementEventSender.sendEvents(any(), any(), any())
            } just runs

            nidJobServiceManager.startJob(mockedSetup.mockedApplication, "clientKey")
            nidJobServiceManager.setTestEventSender(replacementEventSender)

            nidJobServiceManager.sendEvents(forceSendEvents = true)

            // The replacement event sender should now be used instead of the original one.
            verify { replacementEventSender.sendEvents(any(), any(), any()) }
            verify(exactly = 0) {
                mockedSetup.mockedEventSender.sendEvents(any(), any(), any())
            }
        }

    internal data class MockedServices(
        val nidJobServiceManager: NIDJobServiceManager,
        val mockedApplication: Application,
        val mockedLogger: NIDLogWrapper,
        val mockedEventSender: NIDSendingService,
        val mockedNeuroID: NeuroID,
        val mockedConfigService: ConfigService,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupNIDJobServiceManagerMocks(
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
        lowMemory: Boolean = false,
        gyroAccelCadence: Boolean = false,
        gyroAccelCadenceTime: Long = 200L,
    ): MockedServices {
        val mockedApplication = getMockedApplication(lowMemory)
        val logger = getMockedLogger()
        val mockedEventSender = getMockEventSender()
        val mockedNeuroID = getMockedNeuroID()
        every { mockedNeuroID.incrementPacketNumber() } just runs

        val nidRemoteConfigService = getMockedConfigService()
        every { nidRemoteConfigService.configCache } returns
            NIDRemoteConfig(
                gyroAccelCadence = gyroAccelCadence,
                gyroAccelCadenceTime = gyroAccelCadenceTime,
            )

        val nidJobServiceManager =
            NIDJobServiceManager(
                mockedNeuroID,
                getMockedDatastoreManager(),
                mockedEventSender,
                logger,
                nidRemoteConfigService,
                dispatcher,
            )

        return MockedServices(
            nidJobServiceManager,
            mockedApplication,
            logger,
            mockedEventSender,
            mockedNeuroID,
            nidRemoteConfigService,
        )
    }

    private fun getMockedDatastoreManager(): NIDDataStoreManager {
        val dataStoreManager = mockk<NIDDataStoreManager>()
        val event = NIDEventModel(type = "TEST_EVENT", ts = 1)
        coEvery { dataStoreManager.getAllEvents() } returns listOf(event)
        return dataStoreManager
    }

    private fun getMockedApplication(lowMemory: Boolean = false): Application {
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(any()) } returns listOf()
        every { sensorManager.unregisterListener(any<NIDSensorGenListener>()) } just runs
        every { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>(), any<Int>()) } returns true

        val application = mockk<Application>()
        every { application.getSystemService(any()) } returns sensorManager

        val sharedPreferences = mockk<SharedPreferences>()
        every { sharedPreferences.getString(any(), any()) } returns "test"
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences

        val activityManager = mockk<ActivityManager>()
        every { application.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } answers {
            val memoryInfo = firstArg<ActivityManager.MemoryInfo>()
            memoryInfo.lowMemory = lowMemory
            memoryInfo.totalMem = 1000L
            memoryInfo.availMem = 500L
            memoryInfo.threshold = 100L
        }

        return application
    }

    private fun getMockEventSender(): NIDSendingService {
        val eventSender = mockk<NIDEventSender>()

        every { eventSender.sendEvents(any<String>(), any<List<NIDEventModel>>(), any<NIDResponseCallBack<Any>>()) } just runs

        return eventSender
    }
}
