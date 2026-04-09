package com.neuroid.tracker.service

import android.app.Activity
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
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
    // isScreenshotPath tests
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotPath_withScreenshotKeyword_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/Screenshots/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenshotInFilename_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/DCIM/Screenshot_20260401.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenCaptureKeyword_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/screen_capture/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenShotHyphen_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/screen-shot/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenShotSpace_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/screen shot/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_caseInsensitive_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/SCREENSHOTS/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenCaptureHyphen_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/Screen-Capture/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_normalPhoto_returnsFalse() {
        assertFalse(service.isScreenshotPath("/storage/emulated/0/DCIM/Camera/IMG_20260401.jpg"))
    }

    @Test
    fun test_isScreenshotPath_downloadedImage_returnsFalse() {
        assertFalse(service.isScreenshotPath("/storage/emulated/0/Download/wallpaper.png"))
    }

    @Test
    fun test_isScreenshotPath_emptyPath_returnsFalse() {
        assertFalse(service.isScreenshotPath(""))
    }

    @Test
    fun test_isScreenshotPath_withScreenShotUnderscore_returnsTrue() {
        assertTrue(service.isScreenshotPath("/storage/emulated/0/Pictures/screen_shot/IMG_001.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenCapture_returnsTrue() {
        assertTrue(service.isScreenshotPath("/some/path/screencapture_20260401.png"))
    }

    @Test
    fun test_isScreenshotPath_withScreenCaptureSpace_returnsTrue() {
        assertTrue(service.isScreenshotPath("/some/path/screen capture/image.png"))
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri tests — Strategy 1: URI string contains keyword
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy1_uriContainsScreenshot_returnsTrue() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/screenshot_20260401.png"

        val activity = createMockedActivityWithContentResolver()

        assertTrue(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy1_uriContainsScreenCapture_returnsTrue() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/screencapture/12345"

        val activity = createMockedActivityWithContentResolver()

        assertTrue(service.isScreenshotUri(activity, uri))
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri tests — Strategy 2: DISPLAY_NAME query
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy2_displayNameContainsScreenshot_returnsTrue() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val cursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "Screenshot_20260401_123456.png",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns cursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertTrue(service.isScreenshotUri(activity, uri))
        verify { cursor.close() }
    }

    @Test
    fun test_isScreenshotUri_strategy2_displayNameNoScreenshot_continuesChecking() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401_camera.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns displayCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        // No strategy finds a match
        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy2_displayNameNull_continuesChecking() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val nullCursor = createMockedCursor(MediaStore.Images.Media.DISPLAY_NAME, null)

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns nullCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy2_queryReturnsNull_continuesChecking() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns null

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy2_queryThrowsException_logsErrorAndContinues() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } throws SecurityException("No permission")

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
        verify { logger.e(any(), any()) }
    }

    @Test
    fun test_isScreenshotUri_strategy2_emptyCursor_continuesChecking() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val emptyCursor = mockk<Cursor>()
        every { emptyCursor.moveToFirst() } returns false
        every { emptyCursor.close() } just runs

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns emptyCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy2_columnIndexNegative_continuesChecking() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val cursor = mockk<Cursor>()
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(any()) } returns -1
        every { cursor.close() } just runs

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns cursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri — all strategies fail
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_allStrategiesFail_returnsFalse() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns null

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
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

    @Test
    fun test_teardown_unregistersContentObserver() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU // API 33

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)

        service.setupScreenCaptureListener(activity, mockk(relaxed = true), mockk(relaxed = true))
        service.teardownScreenCaptureListener()

        verify { contentResolver.unregisterContentObserver(any()) }
    }

    // -----------------------------------------------------------------------
    // setupScreenCaptureListener — ContentObserver registration
    // -----------------------------------------------------------------------

    @Test
    fun test_setupScreenCaptureListener_registersContentObserver() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)

        service.setupScreenCaptureListener(activity, mockk(relaxed = true), mockk(relaxed = true))

        verify {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                any(),
            )
        }

        verify { logger.d(any(), match { it.contains("Screen capture listener registered") }) }
    }

    @Test
    fun test_setupScreenCaptureListener_cleansUpPreviousRegistration() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        // Setup twice — the second call should teardown the first
        service.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))
        service.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        verify(atLeast = 1) { contentResolver.unregisterContentObserver(any()) }
    }

    // -----------------------------------------------------------------------
    // ContentObserver onChange — integration tests
    // -----------------------------------------------------------------------

    @Test
    fun test_contentObserver_onChange_withScreenshotUri_callsListener() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val screenshotUri = mockk<Uri>()
        every { screenshotUri.toString() } returns "content://media/external/images/screenshot_20260401.png"

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        // Capture the ContentObserver that was registered
        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        observerSlot[0].onChange(false, screenshotUri)
        advanceUntilIdle()

        verify { listener.onScreenCaptured() }
    }

    @Test
    fun test_contentObserver_onChange_withNonScreenshotUri_doesNotCallListener() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val normalUri = mockk<Uri>()
        every { normalUri.toString() } returns "content://media/external/images/media/99999"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401_camera.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.query(normalUri, any(), null, null, null) } returns displayCursor
        every { contentResolver.registerContentObserver(any(), any(), any()) } just runs

        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        observerSlot[0].onChange(false, normalUri)
        advanceUntilIdle()

        verify(exactly = 0) { listener.onScreenCaptured() }
    }

    @Test
    fun test_contentObserver_onChange_withNullUri_doesNotCallListener() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        observerSlot[0].onChange(false, null)
        advanceUntilIdle()

        verify(exactly = 0) { listener.onScreenCaptured() }
    }

    @Test
    fun test_contentObserver_onChange_duplicateUri_doesNotCallListenerTwice() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val screenshotUri = mockk<Uri>()
        every { screenshotUri.toString() } returns "content://media/external/images/screenshot_001.png"

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        // Trigger onChange twice with same URI — second should be deduped
        observerSlot[0].onChange(false, screenshotUri)
        advanceUntilIdle()
        observerSlot[0].onChange(false, screenshotUri)
        advanceUntilIdle()

        verify(exactly = 1) { listener.onScreenCaptured() }
    }

    @Test
    fun test_contentObserver_onChange_exceptionInQuery_doesNotCrash() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/99999"

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every {
            contentResolver.query(uri, any(), null, null, null)
        } throws RuntimeException("Test exception")

        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        observerSlot[0].onChange(false, uri)
        advanceUntilIdle()

        verify(exactly = 0) { listener.onScreenCaptured() }
        verify { logger.e(any(), any()) }
    }

    // -----------------------------------------------------------------------
    // teardown cancels coroutine scope
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_afterSetup_cancelsScope_andCanResetup() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val activity = createMockedActivityWithContentResolver(contentResolver)

        service.setupScreenCaptureListener(activity, mockk(relaxed = true), mockk(relaxed = true))
        service.teardownScreenCaptureListener()

        verify { contentResolver.unregisterContentObserver(any()) }

        // After teardown, setting up again should work (scope is recreated)
        service.setupScreenCaptureListener(activity, mockk(relaxed = true), mockk(relaxed = true))
        service.teardownScreenCaptureListener()
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri tests — Strategy 3: RELATIVE_PATH (API 29+)
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy3_relativePathContainsScreenshot_returnsTrue() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val relPathCursor = createMockedCursor(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/Screenshots/",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } returns relPathCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertTrue(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy3_relativePathNull_continuesChecking() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val relPathCursor = createMockedCursor(
            MediaStore.Images.Media.RELATIVE_PATH,
            null,
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } returns relPathCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy3_relativePathNoScreenshot_continuesChecking() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val relPathCursor = createMockedCursor(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/Camera/",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } returns relPathCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy3_queryThrowsException_logsErrorAndContinues() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } throws SecurityException("No permission")

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
        verify { logger.e(any(), any()) }
    }

    @Test
    fun test_isScreenshotUri_strategy3_queryReturnsNull_continuesChecking() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } returns null

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy3_columnIndexNegative_continuesChecking() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.Q

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val relPathCursor = mockk<Cursor>()
        every { relPathCursor.moveToFirst() } returns true
        every { relPathCursor.getColumnIndex(any()) } returns -1
        every { relPathCursor.close() } just runs

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        } returns relPathCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri tests — Strategy 4: DATA column (API < 29)
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy4_dataColumnContainsScreenshot_returnsTrue() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O // API 26

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        @Suppress("deprecation")
        val dataCursor = createMockedCursor(
            MediaStore.Images.Media.DATA,
            "/storage/emulated/0/Pictures/Screenshots/IMG_001.png",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns dataCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertTrue(service26.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy4_dataColumnNoScreenshot_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        @Suppress("deprecation")
        val dataCursor = createMockedCursor(
            MediaStore.Images.Media.DATA,
            "/storage/emulated/0/DCIM/Camera/IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns dataCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy4_dataColumnNull_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        @Suppress("deprecation")
        val dataCursor = createMockedCursor(
            MediaStore.Images.Media.DATA,
            null,
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns dataCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy4_queryThrowsException_logsErrorAndReturnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } throws SecurityException("No permission")

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))
        verify { logger.e(any(), any()) }
    }

    @Test
    fun test_isScreenshotUri_strategy4_queryReturnsNull_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns null

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))
    }

    @Test
    fun test_isScreenshotUri_strategy4_columnIndexNegative_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        val dataCursor = mockk<Cursor>()
        every { dataCursor.moveToFirst() } returns true
        every { dataCursor.getColumnIndex(any()) } returns -1
        every { dataCursor.close() } just runs

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns dataCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri — Strategy 3 skipped when API < 29
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy3_skippedWhenApiBelowQ() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val service26 = NIDScreenCaptureService(logger, Dispatchers.Unconfined, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        // Only DISPLAY_NAME and DATA queries - no RELATIVE_PATH query should happen
        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "IMG_20260401.jpg",
        )

        @Suppress("deprecation")
        val dataCursor = createMockedCursor(
            MediaStore.Images.Media.DATA,
            "/storage/emulated/0/DCIM/Camera/IMG_20260401.jpg",
        )

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor
        @Suppress("deprecation")
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        } returns dataCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service26.isScreenshotUri(activity, uri))

        // Verify RELATIVE_PATH was never queried
        verify(exactly = 0) {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
        }
    }

    // -----------------------------------------------------------------------
    // Teardown — exception handling in ContentObserver unregister
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_contentObserverUnregisterThrows_doesNotCrash() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every {
            contentResolver.unregisterContentObserver(any())
        } throws RuntimeException("Already unregistered")

        val activity = createMockedActivityWithContentResolver(contentResolver)

        service.setupScreenCaptureListener(activity, mockk(relaxed = true), mockk(relaxed = true))
        // Should not throw
        service.teardownScreenCaptureListener()

        verify { logger.e(any(), any()) }
    }

    // -----------------------------------------------------------------------
    // isScreenshotUri — Strategy 2 DISPLAY_NAME getString returns null
    // -----------------------------------------------------------------------

    @Test
    fun test_isScreenshotUri_strategy2_displayNameGetStringReturnsNull_continuesChecking() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        // Cursor where moveToFirst returns true, column index is valid, but getString returns null
        val cursor = mockk<Cursor>()
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME) } returns 0
        every { cursor.getString(0) } returns null
        every { cursor.close() } just runs

        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns cursor

        val activity = createMockedActivityWithContentResolver(contentResolver)

        assertFalse(service.isScreenshotUri(activity, uri))
    }

    // -----------------------------------------------------------------------
    // ContentObserver onChange — screenshot detected via DISPLAY_NAME
    // -----------------------------------------------------------------------

    @Test
    fun test_contentObserver_onChange_screenshotDetectedViaDisplayName_callsListener() = runTest {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testService = NIDScreenCaptureService(logger, testDispatcher, mockSdkVersionProvider)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/external/images/media/12345"

        val displayCursor = createMockedCursor(
            MediaStore.Images.Media.DISPLAY_NAME,
            "Screenshot_20260401_123456.png",
        )

        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
        } returns displayCursor

        val activity = createMockedActivityWithContentResolver(contentResolver)
        val listener = mockk<NIDScreenCaptureService.ScreenCaptureListener>(relaxed = true)

        testService.setupScreenCaptureListener(activity, listener, mockk(relaxed = true))

        val observerSlot = mutableListOf<android.database.ContentObserver>()
        verify {
            contentResolver.registerContentObserver(any(), any(), capture(observerSlot))
        }

        assertTrue(observerSlot.isNotEmpty())
        observerSlot[0].onChange(false, uri)
        advanceUntilIdle()

        verify { listener.onScreenCaptured() }
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







