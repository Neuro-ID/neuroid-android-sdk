package com.neuroid.tracker.events

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDWindowCallback
import com.neuroid.tracker.callbacks.NIDFocusChangeListener
import com.neuroid.tracker.callbacks.NIDLayoutChangeListener
import java.util.*

fun registerViewsEventsForActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))

    val callBack = activity.window.callback
    val touchManager = NIDTouchEventManager(viewMainContainer as ViewGroup)
    activity.window.callback = NIDWindowCallback(callBack, touchManager)

    val hashCodeAct = activity.hashCode()
    val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

    android.os.Handler(Looper.getMainLooper()).postDelayed({
        identifyAllViews(viewMainContainer, guid) }, 400)
}

fun unRegisterListenerFromActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.removeOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))
}