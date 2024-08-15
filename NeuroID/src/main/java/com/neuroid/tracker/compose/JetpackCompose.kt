package com.neuroid.tracker.compose

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.events.REGISTER_TARGET
import com.neuroid.tracker.events.TOUCH_END
import com.neuroid.tracker.events.TOUCH_START
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
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

    fun trackElementOnChange(
        oldState: String,
        newState: String,
        elementName: String,
        pageName: String,
    )

    fun trackButtonTap(
        elementName: String,
        pageName: String,
    )

    @Composable
    fun trackPage(pageName: String)

    @Composable
    fun trackPageElements(
        pageName: String,
        elements: Array<Pair<String, String>>,
    )
}

class JetpackComposeImpl(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
    val sdkMap: Map<String, Boolean> =
        mapOf(
            "jetpackCompose" to true,
        ),
) : JetpackCompose {
    /**
     * Track a compose button onClick
     */
    override fun trackButtonTap(
        elementName: String,
        pageName: String,
    ) {
        if (elementName.isEmpty() || pageName.isEmpty()) {
            Log.d("NeuroID Debug Event", "Skipping button tracking since either name: $elementName or $pageName is empty")
            return
        }

        neuroID.captureEvent(
            type = TOUCH_START,
            ec = pageName,
            tgs = elementName,
            tg = sdkMap,
            attrs =
                listOf(
                    sdkMap,
                ),
            synthetic = true,
        )

        neuroID.captureEvent(
            type = TOUCH_END,
            ec = pageName,
            tgs = elementName,
            tg = sdkMap,
            attrs =
                listOf(
                    sdkMap,
                ),
            synthetic = true,
        )
    }

    /**
     * Track a compose page load/unload
     */
    @Composable
    override fun trackPage(pageName: String) {
        DisposableEffect(Unit) {
            captureComposeWindowEvent(pageName = pageName, WINDOW_LOAD)
            onDispose {
                captureComposeWindowEvent(pageName = pageName, WINDOW_UNLOAD)
            }
        }
    }

    internal fun captureComposeWindowEvent(
        pageName: String,
        type: String,
    ) {
        neuroID.captureEvent(
            type = type,
            ec = pageName,
            attrs =
                listOf(
                    sdkMap,
                ),
        )
    }

    /**
     * Track a compose page and the associated elements on the page
     */
    @Composable
    override fun trackPageElements(
        pageName: String,
        elements: Array<Pair<String, String>>,
    ) {
        trackPage(pageName = pageName)

        elements.forEach { pair ->
            trackElement(elementState = pair.second, elementName = pair.first, pageName = pageName)
        }
    }

    /**
     * Track a compose element on the page and its state changes through LaunchedEffect listeners
     */
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

        // Runs only on init launch so we can capture the target registration
        LaunchedEffect(Unit) {
            neuroID.captureEvent(
                type = REGISTER_TARGET,
                ec = pageName,
                eid = elementName,
                tgs = elementName,
                en = elementName,
                v = "S~C~~${elementState.length}",
                hv = elementState.getSHA256withSalt().take(8),
                rts = "true",
                attrs =
                    listOf(
                        sdkMap,
                    ),
                etn = INPUT,
                et = "Edittext",
            )
        }

        // runs whenever the element state changes (i.e. text changes)
        LaunchedEffect(elementState) {
            if (!neuroID.isStopped() && elementState != previousText) {
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
                        tgs = elementName,
                        ec = pageName,
                        v = "S~C~~${clipboardContent.length}",
                        hv = clipboardContent.getSHA256withSalt().take(8),
                        attrs =
                            listOf(
                                mapOf(
                                    "clipboardText" to "S~C~~${clipboardContent.length}",
                                ),
                                sdkMap,
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
                    tgs = elementName,
                    ec = pageName,
                    v = "S~C~~${elementState.length}",
                    hv = elementState.getSHA256withSalt().take(8),
                    attrs =
                        listOf(
                            sdkMap,
                        ),
                )
                previousText = elementState
            }
        }
    }

    /**
     * Added as a secondary method in case the [trackElement] method is not available to use
     */
    override fun trackElementOnChange(
        oldState: String,
        newState: String,
        elementName: String,
        pageName: String,
    ) {
        if (!neuroID.isStopped() && newState != oldState) {
            val clipboard = neuroID.getClipboardManagerInstance()
            val clipData = clipboard?.primaryClip

            var pastedText = ""
            if (clipData != null && clipData.itemCount > 0) {
                try {
                    pastedText = clipData.getItemAt(0).text.toString()
                } catch (e: Exception) {
                    e.message?.let {
                        logger.e("Activity", it)
                    }
                }
            }

            val changeText = neuroID.nidComposeTextWatcher.getTextChange(oldState, newState)
            val clipboardContent = pastedText
            // check for paste, send paste event if so
            if (neuroID.nidComposeTextWatcher.isPaste(changeText, clipboardContent, oldState.hashCode())) {
                neuroID.captureEvent(
                    type = PASTE,
                    tg =
                        hashMapOf(
                            "attr" to getAttrJson(newState),
                            "et" to "text",
                        ),
                    tgs = elementName,
                    ec = pageName,
                    v = "S~C~~${clipboardContent.length}",
                    hv = clipboardContent.getSHA256withSalt().take(8),
                    attrs =
                        listOf(
                            mapOf(
                                "clipboardText" to "S~C~~${clipboardContent.length}",
                            ),
                            sdkMap,
                        ),
                )
            }
            neuroID.captureEvent(
                type = INPUT,
                tg =
                    hashMapOf(
                        "attr" to getAttrJson(newState),
                        "etn" to INPUT,
                        "et" to "text",
                    ),
                tgs = elementName,
                ec = pageName,
                v = "S~C~~${newState.length}",
                hv = newState.getSHA256withSalt().take(8),
                attrs =
                    listOf(
                        sdkMap,
                    ),
            )
        }
    }
}
