package com.neuroid.tracker.events

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDWindowCallback
import com.neuroid.tracker.callbacks.NIDFocusChangeListener
import com.neuroid.tracker.callbacks.NIDLayoutChangeListener

fun registerViewsEventsForActivity(activity: Activity) {
    val screenName = activity::class.java.simpleName

    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer, screenName))

    val callBack = activity.window.callback
    val touchManager = NIDTouchEventManager(viewMainContainer as ViewGroup)
    activity.window.callback = NIDWindowCallback(callBack, touchManager)
}

fun unRegisterListenerFromActivity(activity: Activity) {
    val screenName = activity::class.java.simpleName

    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.removeOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer, screenName))
}