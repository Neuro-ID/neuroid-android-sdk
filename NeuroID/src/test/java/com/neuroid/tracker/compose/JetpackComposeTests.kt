package com.neuroid.tracker.compose

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.TOUCH_END
import com.neuroid.tracker.events.TOUCH_START
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class JetpackComposeTests {
    private lateinit var logger: NIDLogWrapper
    private lateinit var neuroID: NeuroID
    private lateinit var composeTracking: JetpackComposeImpl

    @Before
    fun setup() {
        logger = getMockedLogger()
        neuroID = getMockedNeuroID()

        composeTracking =
            JetpackComposeImpl(
                neuroID,
                logger,
            )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun test_trackElement() {
        // TODO: Not yet implemented - Placeholder
        assert(true)
    }

    @Test
    fun test_trackButtonTap() {
        val elementName = "myBtn"
        val pageName = "myPage"

        composeTracking.trackButtonTap(
            elementName,
            pageName,
        )

        verifyCaptureEvent(
            neuroID,
            TOUCH_START,
            1,
            ec = pageName,
            tgs = elementName,
            synthetic = true,
        )

        verifyCaptureEvent(
            neuroID,
            TOUCH_END,
            1,
            ec = pageName,
            tgs = elementName,
            synthetic = true,
        )
    }

    @Test
    fun test_trackButtonTap_fail() {
        val elementName = ""
        val pageName = "myPage"

        composeTracking.trackButtonTap(
            elementName,
            pageName,
        )

        verifyCaptureEvent(
            neuroID,
            LOG,
            1,
            level = "WARN",
        )
    }

    @Test
    fun test_trackPage() {
        val pageName = "myPage"
        composeTracking.captureComposeWindowEvent(pageName, WINDOW_LOAD)
        verifyCaptureEvent(
            neuroID,
            WINDOW_LOAD,
            1,
            ec = pageName,
        )

        composeTracking.captureComposeWindowEvent(pageName, WINDOW_UNLOAD)
        verifyCaptureEvent(
            neuroID,
            WINDOW_UNLOAD,
            1,
            ec = pageName,
        )
    }
}
