package com.neuroid.tracker.callbacks

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDLayoutChangeListener(
    private val nidDataStoreManager: NIDDataStoreManager,
    private val viewMainContainer: View
) : OnGlobalLayoutListener {
    var currentWidth = 0
    var currentHeight = 0

    override fun onGlobalLayout() {
        if (currentWidth == 0 && currentHeight == 0) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
        }

        if (currentWidth != viewMainContainer.width || currentHeight != viewMainContainer.height) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
            nidDataStoreManager
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