package com.neuroid.tracker.utils

import com.neuroid.tracker.NeuroID

class NIDComposeTextWatcherUtils(val neuroID: NeuroID) {
    fun isPaste(
        changeText: TextChange,
        clipboardContent: String,
        lastPastedHashCode: Int,
    ): Boolean {
        if (clipboardContent.isNotEmpty()) {
            // send paste event here!
            val pasteCount = clipboardContent.length
            if (changeText.changedText.contains(clipboardContent) && (pasteCount == changeText.count)) {
                // yeah we most likely have a paste event
                val currentPastedHashCode = clipboardContent.hashCode()
                // check for duplicate paste content
                if (currentPastedHashCode != lastPastedHashCode) {
                    // return true to indicate paste event
                    return true
                }
            }
        }
        // no paste event if we get here
        return false
    }

    fun getTextChange(
        previousText: String,
        currentText: String,
    ): TextChange {
        // equivalent of the TextWatcher, we need the count and changedText
        val start = currentText.commonPrefixWith(previousText).length
        val count = currentText.length - start

        var changedText = ""
        if (count > 0) {
            changedText = currentText.substring(start, start + count)
        }
        return TextChange(start, count, changedText)
    }

    data class TextChange(val start: Int, val count: Int, val changedText: String)
}
