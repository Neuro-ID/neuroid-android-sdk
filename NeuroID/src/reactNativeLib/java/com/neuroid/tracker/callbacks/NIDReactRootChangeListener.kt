package com.neuroid.tracker.callbacks

import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.neuroid.tracker.events.identifyAllViews

class NIDReactRootChangeListener(
    private val viewMainContainer: ViewGroup,
    private val nameActivity: String
) : ViewTreeObserver.OnGlobalLayoutListener  {
    override fun onGlobalLayout() {
        identifyAllViews(viewMainContainer, nameActivity)
        viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(this@NIDReactRootChangeListener)
    }
}