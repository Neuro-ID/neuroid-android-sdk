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
import com.neuroid.tracker.events.LOG
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
    internal fun captureComposeTouchEvent(
        type: String,
        elementName: String,
        pageName: String,
    ) {
        neuroID.captureEvent(
            type = type,
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

    internal fun captureComposeInputEvent(
        elementState: String,
        elementName: String,
        pageName: String,
    ) {
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
    }

    internal fun captureComposePasteEvent(
        elementState: String,
        elementName: String,
        pageName: String,
        clipboardContent: String,
    ) {
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
    }

    internal fun captureComposeRegisterTargetEvent(
        elementState: String? = null,
        elementName: String,
        pageName: String,
        targetType: String = "Edittext",
        rts: String = "true",
    ) {
        neuroID.captureEvent(
            type = REGISTER_TARGET,
            ec = pageName,
            eid = elementName,
            tgs = elementName,
            en = elementName,
            v =
                if (elementState != null) {
                    "S~C~~${elementState.length}"
                } else {
                    null
                },
            hv = elementState?.getSHA256withSalt()?.take(8),
            rts = rts,
            attrs =
                listOf(
                    sdkMap,
                ),
            etn = INPUT,
            et = targetType,
        )
    }

    /**
     * Track a compose button onClick
     */
    override fun trackButtonTap(
        elementName: String,
        pageName: String,
    ) {
        if (elementName.isEmpty() || pageName.isEmpty()) {
            Log.d("NeuroID Compose", "Skipping Compose button tracking since either name: $elementName or $pageName is empty")
            neuroID.captureEvent(
                type = LOG,
                m = "Skipping Compose button tracking since either name: $elementName or $pageName is empty",
                level = "WARN",
            )
            return
        }

        captureComposeRegisterTargetEvent(
            null,
            elementName,
            pageName,
            targetType = "Button",
            rts = "targetInteractionEvent",
        )

        captureComposeTouchEvent(
            TOUCH_START,
            elementName,
            pageName,
        )

        captureComposeTouchEvent(
            TOUCH_END,
            elementName,
            pageName,
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
            captureComposeRegisterTargetEvent(
                elementState,
                elementName,
                pageName,
            )
        }

        // runs whenever the element state changes (i.e. text changes)
        LaunchedEffect(elementState) {
            if (!neuroID.isStopped() && elementState != previousText) {
                val changeText = neuroID.nidComposeTextWatcher.getTextChange(previousText, elementState)
                val clipboardContent = clipboardManager.getText()?.text ?: ""
                // check for paste, send paste event if so
                if (neuroID.nidComposeTextWatcher.isPaste(changeText, clipboardContent, lastPastedHashCode)) {
                    captureComposePasteEvent(
                        elementState,
                        elementName,
                        pageName,
                        clipboardContent,
                    )
                    lastPastedHashCode = clipboardContent.hashCode()
                }

                captureComposeInputEvent(
                    elementState,
                    elementName,
                    pageName,
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
            // This will create an excess number of register target events however DS will
            // handle it on their side

            captureComposeRegisterTargetEvent(
                newState,
                elementName,
                pageName,
                rts = "targetInteractionEvent",
            )

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
                captureComposePasteEvent(
                    newState,
                    elementName,
                    pageName,
                    clipboardContent,
                )
            }

            captureComposeInputEvent(
                newState,
                elementName,
                pageName,
            )
        }
    }
}
