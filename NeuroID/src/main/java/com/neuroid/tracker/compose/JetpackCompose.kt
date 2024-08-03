package com.neuroid.tracker.compose

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.TOUCH_END
import com.neuroid.tracker.events.TOUCH_START
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

    fun trackPage(pageName: String)
}

class JetpackComposeImpl(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
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
        val sdkMap =
            mapOf(
                "jetpackCompose" to true,
            )

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

    override fun trackPage(pageName: String) {
        TODO("Not yet implemented - Placeholder")
    }
}
