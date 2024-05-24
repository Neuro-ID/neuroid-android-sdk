package com.neuroid.tracker.callbacks

import android.view.MenuItem
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CONTEXT_MENU
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class NIDContextMenuCallbacksUnitTests {
    // NOTE: Only testing the `saveEvent` function because everything else is the same for
    //          the NIDTextContextMenuCallbacks and NIDLongPressContextMenuCallbacks classes
    //          and we couldn't create an instance of the underlying abstract NIDContextMenuCallBacks
    @Test
    fun test_saveEvent()  {
        val mocks = getCallbackMocks()
        val callbackClass =
            NIDTextContextMenuCallbacks(
                mocks.first,
                mocks.second,
                null,
            )

        val mockedMenuItem = mockk<MenuItem>()
        every { mockedMenuItem.itemId } returns 16908322

        callbackClass.onActionItemClicked(
            null,
            mockedMenuItem,
        )

        verifyCaptureEvent(
            mocks.first,
            CONTEXT_MENU,
            1,
            attrs =
                listOf(
                    mapOf(
                        "option" to "${16908322}",
                        "item" to mockedMenuItem.toString(),
                        "type" to "PASTE",
                    ),
                ),
        )
    }

    fun getCallbackMocks(): Pair<NeuroID, NIDLogWrapper> {
        val mockedNeuroID = getMockedNeuroID()
        val mockedLogger = getMockedLogger()

        return Pair(
            mockedNeuroID,
            mockedLogger,
        )
    }
}
