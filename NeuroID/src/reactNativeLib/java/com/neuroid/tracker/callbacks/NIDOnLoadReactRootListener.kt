package com.neuroid.tracker.callbacks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.events.registerViewsEventsForActivity
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.getReactRoot
import com.neuroid.tracker.utils.hasFragments

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

                        val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
                        fragManager?.let {
                            if (it.hasFragments().not()) {
                                registerViewsEventsForActivity(activity)
                            }
                        }

                        viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        }
    }
}