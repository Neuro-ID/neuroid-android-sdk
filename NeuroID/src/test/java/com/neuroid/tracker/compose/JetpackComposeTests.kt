package com.neuroid.tracker.compose

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class JetpackComposeTests {
    private lateinit var logger: NIDLogWrapper
    private lateinit var neuroID: NeuroID
    private lateinit var composeTracking: JetpackCompose

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
        // TODO: Not yet implemented - Placeholder
        assert(true)
    }

    @Test
    fun test_trackPage() {
        // TODO: Not yet implemented - Placeholder
        assert(true)
    }
}
