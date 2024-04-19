package com.neuroid.tracker.utils

import android.text.Editable
import android.text.TextWatcher
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson

class NIDTextWatcher(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
    private val idName: String,
    val className: String? = "",
    val startingHashValue: String? = "",
) : TextWatcher {
    private var lastSize = 0
    private var lastHashValue = startingHashValue
    private var lastPastedHashValue: String? = ""

    override fun beforeTextChanged(
        p0: CharSequence?,
        p1: Int,
        p2: Int,
        p3: Int,
    ) {
        // No operation
    }

    override fun onTextChanged(
        sequence: CharSequence?,
        start: Int,
        before: Int,
        count: Int,
    ) {
        // Check if the change is due to a paste operation
        val clipboard = NeuroID.getInternalInstance()?.getClipboardManagerInstance()
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            var pastedText = ""
            try {
                pastedText = clipData.getItemAt(0).text.toString()
            } catch (e: Exception) {
                e.message?.let {
                    logger.e("Activity", it)
                }
            }
            val pasteCount = pastedText.length
            if (sequence.toString().contains(pastedText) && (pasteCount == count)) {
                // The change is likely due to a paste operation

                val currentPastedHashValue = sequence?.toString()?.hashCode().toString()
                // Checks if paste operation is duplicated ENG-6236
                if (currentPastedHashValue != lastPastedHashValue) {
                    lastPastedHashValue = sequence?.toString()?.hashCode().toString()

                    if (pastedText.isNotEmpty()) {
                        neuroID.captureEvent(
                            type = PASTE,
                            tg =
                                hashMapOf(
                                    "attr" to getAttrJson(sequence.toString()),
                                    "et" to "text",
                                ),
                            tgs = idName,
                            v = "S~C~~${sequence?.length}",
                            hv = sequence?.toString()?.getSHA256withSalt()?.take(8),
                            attrs =
                                listOf(
                                    mapOf(
                                        "clipboardText" to "S~C~~${pastedText.length}",
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }

    override fun afterTextChanged(sequence: Editable?) {
        val currentHashValue = sequence?.toString()?.getSHA256withSalt()?.take(8)

        if (lastHashValue != currentHashValue) {
            lastHashValue = sequence?.toString()?.getSHA256withSalt()?.take(8)

            neuroID.captureEvent(
                type = INPUT,
                tg =
                    hashMapOf(
                        "attr" to getAttrJson(sequence.toString()),
                        "etn" to INPUT,
                        "et" to "text",
                    ),
                tgs = idName,
                v = "S~C~~${sequence?.length}",
                hv = sequence?.toString()?.getSHA256withSalt()?.take(8),
            )
        }
    }
}
