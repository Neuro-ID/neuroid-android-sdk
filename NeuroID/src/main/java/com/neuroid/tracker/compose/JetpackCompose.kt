package com.neuroid.tracker.compose

import com.neuroid.tracker.NeuroID
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
        TODO("Not yet implemented - Placeholder")
    }

    override fun trackPage(pageName: String) {
        TODO("Not yet implemented - Placeholder")
    }
}
