package com.neuroid.tracker.events

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDWindowCallback
import com.neuroid.tracker.callbacks.NIDFocusChangeListener
import com.neuroid.tracker.callbacks.NIDLayoutChangeListener

fun registerViewsEventsForActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))

    val callBack = activity.window.callback
    val touchManager = NIDTouchEventManager(viewMainContainer as ViewGroup)
    activity.window.callback = NIDWindowCallback(callBack, touchManager)

    android.os.Handler(Looper.getMainLooper()).postDelayed({
        identifyAllViews(viewMainContainer, activity::class.java.simpleName) }, 3000)
}

fun unRegisterListenerFromActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.removeOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))
}