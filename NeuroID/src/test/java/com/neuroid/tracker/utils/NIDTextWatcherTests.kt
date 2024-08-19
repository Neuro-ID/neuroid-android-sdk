package com.neuroid.tracker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.Editable
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDClassUnitTests
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class NIDTextWatcherTests : NeuroIDClassUnitTests() {
    @Test
    fun textWatcher_on_text_changed_no_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 0
        every { clipData.getItemAt(any()) } returns null

        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        NeuroID.getInternalInstance()?.setClipboardManagerInstance(clipboardManager)

        val nidMock = getMockedNeuroID()

        val textWatcher = NIDTextWatcher(nidMock, NIDLogWrapper(), "test", "myclass", "")
        textWatcher.onTextChanged("existing text", 0, 1, 0)

        verifyCaptureEvent(
            nidMock,
            PASTE,
            0,
        )
    }

    @Test
    fun textWatcher_on_text_changed_with_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        NeuroID.getInternalInstance()?.setClipboardManagerInstance(clipboardManager)
        val nidMock = getMockedNeuroID()

        val textWatcher =
            NIDTextWatcher(
                nidMock,
                NIDLogWrapper(),
                "test",
                "myclass",
                "sgs".hashCode().toString(),
            )

        textWatcher.onTextChanged("existing copied text", 10, 1, 11)

        verifyCaptureEvent(
            nidMock,
            PASTE,
            1,
        )
    }

    @Test
    fun textWatcher_after_text_changed() {
        val clipboardManager = mockk<ClipboardManager>()
        NeuroID.getInternalInstance()?.setClipboardManagerInstance(clipboardManager)
        val nidMock = getMockedNeuroID()

        val textWatcher = NIDTextWatcher(nidMock, NIDLogWrapper(), "test", "myclass", "")
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)

        verifyCaptureEvent(
            nidMock,
            INPUT,
            1,
        )
    }

    @Test
    fun textWatcher_on_text_changed_with_duplicate_paste() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData

        NeuroID.getInternalInstance()?.setClipboardManagerInstance(clipboardManager)
        val nidMock = getMockedNeuroID()

        val textWatcher =
            NIDTextWatcher(
                nidMock,
                NIDLogWrapper(),
                "test",
                "myclass",
                "existing text".hashCode().toString(),
            )
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)

        verifyCaptureEvent(
            nidMock,
            PASTE,
            1,
        )
    }

    @Test
    fun textWatcher_after_duplicate_text_changed() {
        val clipboardManager = mockk<ClipboardManager>()
        NeuroID.getInternalInstance()?.setClipboardManagerInstance(clipboardManager)
        val nidMock = getMockedNeuroID()

        val textWatcher = NIDTextWatcher(nidMock, NIDLogWrapper(), "test", "myclass", "")
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)
        textWatcher.afterTextChanged(seq)

        verifyCaptureEvent(
            nidMock,
            INPUT,
            1,
        )
    }
}
