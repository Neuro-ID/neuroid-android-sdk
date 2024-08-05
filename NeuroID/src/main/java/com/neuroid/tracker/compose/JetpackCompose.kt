package com.neuroid.tracker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.TOUCH_END
import com.neuroid.tracker.events.TOUCH_START
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.utils.NIDLogWrapper

interface JetpackCompose {
    fun trackElement(
        elementState: String,
        elementName: String,
        pageName: String,
    )

    fun trackButtonTap(
        elementName: String,
        pageName: String,
    )

    @Composable
    fun trackPage(pageName: String)
}

class JetpackComposeImpl(
    val neuroID: NeuroID, val logger: NIDLogWrapper, val sdkMap: Map<String, Boolean> = mapOf(
        "jetpackCompose" to true,
    )
) : JetpackCompose {
    override fun trackElement(
        elementState: String,
        elementName: String,
        pageName: String,
    ) {
        TODO("Not yet implemented - Placeholder")
    }

    override fun trackButtonTap(
        elementName: String,
        pageName: String,
    ) {


        neuroID.captureEvent(
            type = TOUCH_START,
            ec = pageName,
            tgs = elementName,
            tg = sdkMap,
            attrs = listOf(
                sdkMap,
            ),
            synthetic = true,
        )

        neuroID.captureEvent(
            type = TOUCH_END,
            ec = pageName,
            tgs = elementName,
            tg = sdkMap,
            attrs = listOf(
                sdkMap,
            ),
            synthetic = true,
        )
    }

    @Composable
    override fun trackPage(pageName: String) {
        DisposableEffect(Unit) {
            neuroID.captureEvent(
                type = WINDOW_LOAD, ec = pageName, attrs = listOf(
                    sdkMap
                )
            )
            onDispose {
                neuroID.captureEvent(type = WINDOW_UNLOAD, ec = pageName, attrs = listOf(sdkMap))
            }
        }

    }
}
