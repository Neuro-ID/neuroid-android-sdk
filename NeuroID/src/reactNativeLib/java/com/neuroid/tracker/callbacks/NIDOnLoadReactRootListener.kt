package com.neuroid.tracker.callbacks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.neuroid.tracker.events.registerViewsEventsForActivity
import com.neuroid.tracker.utils.getReactRoot

class NIDOnLoadReactRootListener(
    private val viewMainContainer: View,
    private val activity: Activity
): OnGlobalLayoutListener {
    private var isRegistered = false

    override fun onGlobalLayout() {
        if(isRegistered.not()) {
            if (viewMainContainer is ViewGroup) {
                val reactRootView = getReactRoot(viewMainContainer)
                reactRootView?.let {
                    if (reactRootView.childCount != 0) {
                        isRegistered = true
                        registerViewsEventsForActivity(activity)

                        viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        }
    }
}