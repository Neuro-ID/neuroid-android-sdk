package com.neuroid.tracker.service

import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NIDScreenCaptureServiceTest {
    private lateinit var logger: NIDLogWrapper
    private lateinit var service: NIDScreenCaptureService

    @Before
    fun setup() {
        logger = getMockedLogger()
        service = NIDScreenCaptureService(logger)
    }

    @After
    fun teardown() {
        unmockkAll()
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

    // -----------------------------------------------------------------------
    // teardownScreenCaptureListener — safe to call when nothing is registered
    // -----------------------------------------------------------------------

    @Test
    fun test_teardown_whenNothingRegistered_doesNotThrow() {
        // Should complete without throwing
        service.teardownScreenCaptureListener()
    }

    @Test
    fun test_teardown_calledTwice_doesNotThrow() {
        service.teardownScreenCaptureListener()
        service.teardownScreenCaptureListener()
    }
}

