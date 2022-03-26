package com.neuroid.tracker.callbacks

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.events.identifyAllViews
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.getReactRoot

class NIDLayoutChangeListener(
    private val viewMainContainer: View,
    private val nameScreen: String
): OnGlobalLayoutListener {
    private var currentWidth = 0
    private var currentHeight = 0
    private var isRegistered = false

    override fun onGlobalLayout() {
        if(isRegistered.not()) {
            if (viewMainContainer is ViewGroup) {
                val reactRootView = getReactRoot(viewMainContainer)
                reactRootView?.let {
                    if (reactRootView.childCount != 0) {
                        isRegistered = true
                        identifyAllViews(viewMainContainer, nameScreen)
                    }
                }
            }
        }

        if (currentWidth == 0 && currentHeight == 0) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
        }

        if (currentWidth != viewMainContainer.width || currentHeight != viewMainContainer.height) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_RESIZE,
                        w = currentWidth,
                        h = currentHeight,
                        ts = System.currentTimeMillis()
                    )
                )
        }
    }
}