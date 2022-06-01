package com.neuroid.tracker.events

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDGlobalEventCallback
import java.util.*

fun registerWindowListeners(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    val callBack = activity.window.callback

    if (callBack !is NIDGlobalEventCallback) {
        val nidGlobalEventCallback = NIDGlobalEventCallback(
            callBack,
            NIDTouchEventManager(viewMainContainer as ViewGroup),
            viewMainContainer
        )
        viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(nidGlobalEventCallback)
        viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(
            nidGlobalEventCallback
        )

        activity.window.callback = nidGlobalEventCallback
    }
}

fun registerTargetFromScreen(
    activity: Activity,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true
) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    ) as ViewGroup

    val hashCodeAct = activity.hashCode()
    val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

    android.os.Handler(Looper.getMainLooper()).postDelayed({
        identifyAllViews(viewMainContainer, guid, registerTarget, registerListeners)
    }, 300)
}