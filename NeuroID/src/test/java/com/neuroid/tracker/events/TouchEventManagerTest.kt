package com.neuroid.tracker.events

import android.view.ViewGroup
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class TouchEventManagerTest {
    @Test
    fun testShouldLogMoveEvent() {
        val mockNID = getMockedNeuroID()
        val mockedViewGroup = mockk<ViewGroup>()
        val mockedLogger = mockk<NIDLogWrapper>()
        val mockedTime = mockk<NIDTime>()
        every { mockedTime.getCurrentTimeMillis() } returns 50
        val tem = TouchEventManager(mockedViewGroup, mockNID, mockedLogger, mockedTime)
        assert(!tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 0)
        assert(tem.missCounter == 1)
        every {mockedTime.getCurrentTimeMillis() } returns 51
        assert(tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 1)
        assert(tem.missCounter == 1)
        every {mockedTime.getCurrentTimeMillis() } returns 99
        assert(!tem.shouldRecordMoveEvent())
        assert(tem.hitCounter == 1)
        assert(tem.missCounter == 2)
    }


}