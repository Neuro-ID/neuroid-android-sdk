package com.neuroid.tracker.service

import android.app.Activity
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.core.app.ActivityCompat
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDSdkVersionProvider
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NIDScreenCaptureServiceTest {
    private lateinit var logger: NIDLogWrapper
    private lateinit var mockSdkVersionProvider: NIDSdkVersionProvider
    private lateinit var service: NIDScreenCaptureService

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        logger = getMockedLogger()
        mockSdkVersionProvider = mockk<NIDSdkVersionProvider>()
        // Default to API 33 (TIRAMISU) — below the native ScreenCaptureCallback threshold
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU
        service = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // -----------------------------------------------------------------------
    // Helper: create a mocked Activity with a mocked ContentResolver
    // -----------------------------------------------------------------------

    private fun createMockedActivityWithContentResolver(
        contentResolver: ContentResolver = mockk(relaxed = true),
    ): Activity {
        val activity = mockk<Activity>(relaxed = true)
        every { activity.contentResolver } returns contentResolver
        return activity
    }

    // -----------------------------------------------------------------------
    // Helper: create a mocked Cursor that returns a value for a given column
    // -----------------------------------------------------------------------

    private fun createMockedCursor(columnName: String, value: String?): Cursor {
        val cursor = mockk<Cursor>()
        every { cursor.moveToFirst() } returns (value != null)
        every { cursor.getColumnIndex(columnName) } returns if (value != null) 0 else -1
        if (value != null) {
            every { cursor.getString(0) } returns value
        }
        every { cursor.close() } just runs
        return cursor
    }

    // -----------------------------------------------------------------------
    // teardownScreenCaptureListener — safe to call
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_whenNothingRegistered_doesNotThrow() {
        service.teardownScreenCaptureListener()
    }

    @Test
    fun test_teardown_calledTwice_doesNotThrow() {
        service.teardownScreenCaptureListener()
        service.teardownScreenCaptureListener()
    }

    // -----------------------------------------------------------------------
    // Teardown — API 34+ native callback path via reflection
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_api34_withNullCallbackAndActivity_doesNotThrow() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        val service34 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        // No setup was called, so screenCaptureCallback and registeredActivity are null
        // Should execute the API 34 teardown branch without crashing
        service34.teardownScreenCaptureListener()
    }

    @Test
    fun test_teardown_api34_withCallbackAndActivity_unregistersCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        val service34 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockCallback = mockk<Activity.ScreenCaptureCallback>(relaxed = true)

        // Use reflection to set internal fields
        val callbackField = NIDScreenCaptureService::class.java.getDeclaredField("screenCaptureCallback")
        callbackField.isAccessible = true
        callbackField.set(service34, mockCallback)

        val activityField = NIDScreenCaptureService::class.java.getDeclaredField("registeredActivity")
        activityField.isAccessible = true
        activityField.set(service34, mockActivity)

        service34.teardownScreenCaptureListener()

        verify { mockActivity.unregisterScreenCaptureCallback(mockCallback) }
        verify { logger.d(any(), match { it.contains("Native ScreenCaptureCallback unregistered") }) }
    }

    @Test
    fun test_teardown_api34_unregisterThrowsException_logsError() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        val service34 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockCallback = mockk<Activity.ScreenCaptureCallback>(relaxed = true)

        every { mockActivity.unregisterScreenCaptureCallback(any()) } throws RuntimeException("Failed to unregister")

        // Use reflection to set internal fields
        val callbackField = NIDScreenCaptureService::class.java.getDeclaredField("screenCaptureCallback")
        callbackField.isAccessible = true
        callbackField.set(service34, mockCallback)

        val activityField = NIDScreenCaptureService::class.java.getDeclaredField("registeredActivity")
        activityField.isAccessible = true
        activityField.set(service34, mockActivity)

        service34.teardownScreenCaptureListener()

        verify { logger.e(any(), match { it.contains("Error unregistering native ScreenCaptureCallback") }) }
    }

    @Test
    fun test_teardown_api34_withCallbackButNullActivity_doesNotCallUnregister() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        val service34 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockCallback = mockk<Activity.ScreenCaptureCallback>(relaxed = true)

        // Set only callback, leave activity null
        val callbackField = NIDScreenCaptureService::class.java.getDeclaredField("screenCaptureCallback")
        callbackField.isAccessible = true
        callbackField.set(service34, mockCallback)

        service34.teardownScreenCaptureListener()

        // Should not crash - the null activity check prevents unregister call
    }

    // -----------------------------------------------------------------------
    // ScreenCaptureListener interface
    // -----------------------------------------------------------------------

    @Test
    fun test_screenCaptureListener_canBeMocked() {
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>()
        every { listener.onScreenCaptured() } just runs

        listener.onScreenCaptured()

        verify { listener.onScreenCaptured() }
    }

    // -----------------------------------------------------------------------
    // ScreenRecordingListener interface
    // -----------------------------------------------------------------------

    @Test
    fun test_screenRecordingListener_canBeMocked() {
        val listener = mockk<NIDScreenCaptureService.ScreenRecordingListener>()
        every { listener.onScreenRecorded(any()) } just runs

        listener.onScreenRecorded(true)
        listener.onScreenRecorded(false)

        verify { listener.onScreenRecorded(true) }
        verify { listener.onScreenRecorded(false) }
    }

    // -----------------------------------------------------------------------
    // Screen Recording — setup with permission granted (API 35+)
    // -----------------------------------------------------------------------

    @Test
    fun test_setupScreenCaptureListener_api35_withPermission_registersRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        verify {
            mockWindowManager.addScreenRecordingCallback(any(), any())
        }
    }

    @Test
    fun test_setupScreenCaptureListener_api35_withoutPermission_skipsRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_DENIED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        verify(exactly = 0) {
            mockWindowManager.addScreenRecordingCallback(any(), any())
        }
        verify { logger.d(any(), match { it.contains("DETECT_SCREEN_RECORDING permission not granted") }) }
    }

    @Test
    fun test_setupScreenCaptureListener_belowApi35_doesNotRegisterRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34

        val service34 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service34.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        verify(exactly = 0) {
            mockWindowManager.addScreenRecordingCallback(any(), any())
        }
    }

    // -----------------------------------------------------------------------
    // Screen Recording — callback invocation (recording start and stop)
    // -----------------------------------------------------------------------

    @Test
    fun test_screenRecordingCallback_whenRecordingStarts_callsListenerWithTrue() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        // Capture the Consumer<Int> passed to addScreenRecordingCallback
        val consumerSlot = mutableListOf<java.util.function.Consumer<Int>>()
        every {
            mockWindowManager.addScreenRecordingCallback(any(), capture(consumerSlot))
        } returns 0

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        assertTrue(consumerSlot.isNotEmpty())

        // Simulate recording started (SCREEN_RECORDING_STATE_VISIBLE = 1)
        consumerSlot[0].accept(android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE)

        verify { recordingListener.onScreenRecorded(true) }
        verify { logger.d(any(), match { it.contains("isRecording=true") }) }
    }

    @Test
    fun test_screenRecordingCallback_whenRecordingStops_callsListenerWithFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        val consumerSlot = mutableListOf<java.util.function.Consumer<Int>>()
        every {
            mockWindowManager.addScreenRecordingCallback(any(), capture(consumerSlot))
        } returns 0

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        assertTrue(consumerSlot.isNotEmpty())

        // Simulate recording stopped (SCREEN_RECORDING_STATE_NOT_VISIBLE = 0)
        consumerSlot[0].accept(android.view.WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE)

        verify { recordingListener.onScreenRecorded(false) }
        verify { logger.d(any(), match { it.contains("isRecording=false") }) }
    }

    @Test
    fun test_screenRecordingCallback_recordingStartsThenStops_callsListenerBothTimes() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        val consumerSlot = mutableListOf<java.util.function.Consumer<Int>>()
        every {
            mockWindowManager.addScreenRecordingCallback(any(), capture(consumerSlot))
        } returns 0

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        assertTrue(consumerSlot.isNotEmpty())

        // Simulate recording start then stop
        consumerSlot[0].accept(android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
        consumerSlot[0].accept(android.view.WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE)

        verify(ordering = Ordering.ORDERED) {
            recordingListener.onScreenRecorded(true)
            recordingListener.onScreenRecorded(false)
        }
    }

    // -----------------------------------------------------------------------
    // Screen Recording — teardown (API 35+)
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_api35_withPermission_removesRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)
        service35.teardownScreenCaptureListener()

        verify {
            mockWindowManager.removeScreenRecordingCallback(any())
        }
        verify { logger.d(any(), match { it.contains("Screen recording callback unregistered") }) }
    }

    @Test
    fun test_teardown_api35_withoutPermission_skipsRemoveRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        // Grant permission during setup
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        // Revoke permission before teardown
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_DENIED

        service35.teardownScreenCaptureListener()

        verify(exactly = 0) {
            mockWindowManager.removeScreenRecordingCallback(any())
        }
        verify { logger.d(any(), match { it.contains("DETECT_SCREEN_RECORDING permission not granted, skipping unregister") }) }
    }

    @Test
    fun test_teardown_api35_removeCallbackThrowsException_logsError() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        every {
            mockWindowManager.removeScreenRecordingCallback(any())
        } throws RuntimeException("Failed to remove callback")

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)
        service35.teardownScreenCaptureListener()

        verify { logger.e(any(), match { it.contains("Error unregistering screen recording callback") }) }
    }

    @Test
    fun test_teardown_api35_withNullCallbackAndActivity_doesNotThrow() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        // No setup was called, so screenRecordingCallback and registeredActivity are null
        // Should execute the API 35 teardown branch without crashing
        service35.teardownScreenCaptureListener()
    }

    @Test
    fun test_teardown_api35_withCallbackButNullActivity_doesNotCallRemove() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockConsumer = mockk<java.util.function.Consumer<Int>>(relaxed = true)

        // Use reflection to set only the recording callback, leave activity null
        val callbackField = NIDScreenCaptureService::class.java.getDeclaredField("screenRecordingCallback")
        callbackField.isAccessible = true
        callbackField.set(service35, mockConsumer)

        service35.teardownScreenCaptureListener()

        // Should not crash - the null activity check prevents removeScreenRecordingCallback call
    }

    // -----------------------------------------------------------------------
    // Screen Recording — setup then teardown cycle
    // -----------------------------------------------------------------------

    @Test
    fun test_setupThenTeardown_api35_fullLifecycle() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        val consumerSlot = mutableListOf<java.util.function.Consumer<Int>>()
        every {
            mockWindowManager.addScreenRecordingCallback(any(), capture(consumerSlot))
        } returns 0

        // Setup
        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)
        assertTrue(consumerSlot.isNotEmpty())

        // Simulate recording
        consumerSlot[0].accept(android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
        verify { recordingListener.onScreenRecorded(true) }

        // Teardown
        service35.teardownScreenCaptureListener()
        verify { mockWindowManager.removeScreenRecordingCallback(any()) }
    }

    @Test
    fun test_setup_calledTwice_api35_cleansUpPreviousRecordingCallback() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM

        val service35 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val mockActivity = mockk<Activity>(relaxed = true)
        val mockWindowManager = mockk<android.view.WindowManager>(relaxed = true)
        every { mockActivity.contentResolver } returns mockk(relaxed = true)
        every { mockActivity.windowManager } returns mockWindowManager

        mockkStatic(ActivityCompat::class)
        every {
            ActivityCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val captureListener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)
        val recordingListener = mockk<NIDScreenCaptureService.ScreenRecordingListener>(relaxed = true)

        // Setup twice — the second call should teardown the first
        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)
        service35.setupScreenCaptureListener(mockActivity, captureListener, recordingListener)

        // The first registration should have been cleaned up via teardown
        verify(atLeast = 1) { mockWindowManager.removeScreenRecordingCallback(any()) }
        // Two registrations should have been made
        verify(exactly = 2) { mockWindowManager.addScreenRecordingCallback(any(), any()) }
    }
}







