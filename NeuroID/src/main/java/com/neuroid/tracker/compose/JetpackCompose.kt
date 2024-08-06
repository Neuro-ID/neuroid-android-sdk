package com.neuroid.tracker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson
import com.neuroid.tracker.utils.NIDLogWrapper

interface JetpackCompose {
    @Composable
    fun trackElement(
        elementState: String,
        elementName: String,
        pageName: String,
    )

    fun trackButtonTap(
        elementName: String,
        pageName: String,
    )

    fun trackPage(pageName: String)
}

class JetpackComposeImpl(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
) : JetpackCompose {
    override fun trackButtonTap(
        elementName: String,
        pageName: String,
    ) {
        TODO("Not yet implemented - Placeholder")
    }

    override fun trackPage(pageName: String) {
        TODO("Not yet implemented - Placeholder")
    }

    @Composable
    override fun trackElement(
        elementState: String,
        elementName: String,
        pageName: String,
    ) {
        val clipboardManager = LocalClipboardManager.current
        // save instance of previous text and previous pasted text hashcode
        var previousText by remember { mutableStateOf(elementState) }
        var lastPastedHashCode by remember { mutableStateOf(0) }

        // handle the text changes
        LaunchedEffect(elementState) {
            if (elementState != previousText) {
                val changeText = neuroID.nidComposeTextWatcher.getTextChange(previousText, elementState)
                val clipboardContent = clipboardManager.getText()?.text ?: ""
                // check for paste, send paste event if so
                if (neuroID.nidComposeTextWatcher.isPaste(changeText, clipboardContent, lastPastedHashCode)) {
                    neuroID.captureEvent(
                        type = PASTE,
                        tg =
                            hashMapOf(
                                "attr" to getAttrJson(elementState),
                                "et" to "text",
                            ),
                        tgs = "$pageName:$elementName",
                        v = "S~C~~${clipboardContent.length}",
                        hv = clipboardContent.getSHA256withSalt().take(8),
                        attrs =
                            listOf(
                                mapOf(
                                    "clipboardText" to "S~C~~${clipboardContent.length}",
                                ),
                            ),
                    )
                    lastPastedHashCode = clipboardContent.hashCode()
                }
                neuroID.captureEvent(
                    type = INPUT,
                    tg =
                        hashMapOf(
                            "attr" to getAttrJson(elementState),
                            "etn" to INPUT,
                            "et" to "text",
                        ),
                    tgs = "$pageName:$elementName",
                    v = "S~C~~${elementState.length}",
                    hv = elementState.getSHA256withSalt().take(8),
                )
                previousText = elementState
            }
        }
    }
}
