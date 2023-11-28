package com.neuroid.tracker

import android.content.ClipData
import android.content.ClipboardManager
import android.text.SpannableString
import androidx.core.text.buildSpannedString
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
        every {clipData.itemCount} returns 0
        every {clipData.getItemAt(any())} returns null
        val clipboardManager = mockk<ClipboardManager>()
        every {clipboardManager.primaryClip} returns clipData
        val dataManager = mockk<NIDDataStoreManager>()
        val textWatcher = NIDTextWatcher("test", "myclass", "", clipboardManager, dataManager)
        textWatcher.onTextChanged("a", 0, 1,0)
        verify(exactly = 0) {dataManager.saveEvent(any())}
    }

    @Test
    fun textWatcher_on_text_changed_with_clip() {
        val clipData = mockk<ClipData>()
        every {clipData.itemCount} returns 1
        val clipboardManager = mockk<ClipboardManager>()
        every {clipboardManager.primaryClip} returns clipData
        val dataManager = mockk<NIDDataStoreManager>()
        val textWatcher = NIDTextWatcher("test",
            "myclass",
            "sgs".hashCode().toString(), clipboardManager, dataManager)
        textWatcher.onTextChanged("fhdhdhfhdh", 6, 0,4)
        verify(exactly = 1) {dataManager.saveEvent(any())}
    }
}