package com.neuroid.tracker

import android.content.ClipData
import android.content.ClipboardManager
import android.text.Editable
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDTextWatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NIDTextWatcherTest {
    @Test
    fun textWatcher_on_text_changed_no_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 0
        every { clipData.getItemAt(any()) } returns null
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        val dataManager = mockk<NIDDataStoreManager>()
        val textWatcher = NIDTextWatcher("test", "myclass", "", clipboardManager, dataManager)
        textWatcher.onTextChanged("existing text", 0, 1, 0)
        verify(exactly = 0) { dataManager.saveEvent(any()) }
    }

    @Test
    fun textWatcher_on_text_changed_with_clip() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        val dataManager = mockk<NIDDataStoreManager>()
        every { dataManager.saveEvent(any()) } returns mockk()
        val textWatcher = NIDTextWatcher(
            "test", "myclass", "sgs".hashCode().toString(), clipboardManager, dataManager
        )
        textWatcher.onTextChanged("existing copied text", 10, 1, 11)
        verify(exactly = 1) { dataManager.saveEvent(any()) }
    }

    @Test
    fun textWatcher_after_text_changed() {
        val dataManager = mockk<NIDDataStoreManager>()
        every { dataManager.saveEvent(any()) } returns mockk()
        val clipboardManager = mockk<ClipboardManager>()
        val textWatcher = NIDTextWatcher("test", "myclass", "", clipboardManager, dataManager)
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)
        verify(exactly = 1) { dataManager.saveEvent(any()) }
    }

    @Test
    fun textWatcher_on_text_changed_with_duplicate_paste() {
        val clipData = mockk<ClipData>()
        every { clipData.itemCount } returns 1
        every { clipData.getItemAt(any()).text.toString() } returns "copied text"
        val clipboardManager = mockk<ClipboardManager>()
        every { clipboardManager.primaryClip } returns clipData
        val dataManager = mockk<NIDDataStoreManager>()
        every { dataManager.saveEvent(any()) } returns mockk()
        val textWatcher = NIDTextWatcher(
            "test", "myclass", "existing text".hashCode().toString(), clipboardManager, dataManager
        )
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)
        textWatcher.onTextChanged("existing copied text", 0, 11, 11)
        verify(exactly = 1) { dataManager.saveEvent(any()) }
    }

    @Test
    fun textWatcher_after_duplicate_text_changed() {
        val dataManager = mockk<NIDDataStoreManager>()
        every { dataManager.saveEvent(any()) } returns mockk()
        val clipboardManager = mockk<ClipboardManager>()
        val textWatcher = NIDTextWatcher("test", "myclass", "", clipboardManager, dataManager)
        val seq = mockk<Editable>()
        every { seq.toString() } returns "changed text"
        every { seq.length } returns 12
        textWatcher.afterTextChanged(seq)
        textWatcher.afterTextChanged(seq)
        verify(exactly = 1) { dataManager.saveEvent(any()) }
    }
}