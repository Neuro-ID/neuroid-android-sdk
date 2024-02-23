package com.neuroid.tracker

import android.content.ClipData
import android.content.ClipboardManager
import android.text.Editable
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTextWatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NIDTextWatcherTests : NeuroIDClassUnitTests() {
    @Test
    fun textWatcher_on_text_changed_no_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 0
        every { clipData.getItemAt(any()) } returns null

        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        NeuroID.getInstance()?.setClipboardManagerInstance(clipboardManager)
        val dataStoreMock = setMockedDataStore()

        println("DAA $dataStoreMock")

        val textWatcher = NIDTextWatcher(dataStoreMock, NIDLogWrapper(), "test", "myclass", "")
        textWatcher.onTextChanged("existing text", 0, 1, 0)
        verify(exactly = 0) { NeuroID.getInstance()?.dataStore?.saveEvent(any()) }

    }

    @Test
    fun textWatcher_on_text_changed_with_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        NeuroID.getInstance()?.setClipboardManagerInstance(clipboardManager)
        val dataStoreMock = setMockedDataStore()

        val textWatcher = NIDTextWatcher(dataStoreMock, NIDLogWrapper(),
            "test", "myclass", "sgs".hashCode().toString()
        )

        textWatcher.onTextChanged("existing copied text", 10, 1, 11)
        verify(exactly = 1) { NeuroID.getInstance()?.dataStore?.saveEvent(any()) }

    }

    @Test
    fun textWatcher_after_text_changed() {
        val clipboardManager = mockk<ClipboardManager>()
        NeuroID.getInstance()?.setClipboardManagerInstance(clipboardManager)
        val dataStoreMock = setMockedDataStore()

        val textWatcher = NIDTextWatcher(dataStoreMock, NIDLogWrapper(),"test", "myclass", "")
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)
        verify(exactly = 1) { NeuroID.getInstance()?.dataStore?.saveEvent(any()) }

    }

    @Test
    fun textWatcher_on_text_changed_with_duplicate_paste() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData

        NeuroID.getInstance()?.setClipboardManagerInstance(clipboardManager)
        val dataStoreMock = setMockedDataStore()

        val textWatcher = NIDTextWatcher(dataStoreMock, NIDLogWrapper(),
            "test", "myclass", "existing text".hashCode().toString()
        )
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)
        verify(exactly = 1) { NeuroID.getInstance()?.dataStore?.saveEvent(any()) }

    }

    @Test
    fun textWatcher_after_duplicate_text_changed() {
        val clipboardManager = mockk<ClipboardManager>()
        NeuroID.getInstance()?.setClipboardManagerInstance(clipboardManager)
        val dataStoreMock = setMockedDataStore()

        val textWatcher = NIDTextWatcher(dataStoreMock, NIDLogWrapper(),"test", "myclass", "")
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)
        textWatcher.afterTextChanged(seq)
        verify(exactly = 1) { NeuroID.getInstance()?.dataStore?.saveEvent(any()) }

    }
}