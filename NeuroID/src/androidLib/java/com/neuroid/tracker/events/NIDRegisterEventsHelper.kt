package com.neuroid.tracker.events

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDWindowCallback
import com.neuroid.tracker.callbacks.NIDFocusChangeListener
import com.neuroid.tracker.callbacks.NIDLayoutChangeListener
import java.util.*

fun registerWindowListeners(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    val callBack = activity.window.callback

    if (callBack !is NIDWindowCallback) {
        viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(NIDFocusChangeListener())
        viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))

        val touchManager = NIDTouchEventManager(viewMainContainer as ViewGroup)
        activity.window.callback = NIDWindowCallback(callBack, touchManager)
    }
}

fun registerTargetFromActivity(activity: Activity, changeOrientation: Boolean) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    ) as ViewGroup

    val hashCodeAct = activity.hashCode()
    val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

    android.os.Handler(Looper.getMainLooper()).postDelayed({
        identifyAllViews(viewMainContainer, guid, changeOrientation) }, 400)
}

fun registerTargetFromFragment(activity: Activity, changeOrientation: Boolean) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    val hashCodeAct = activity.hashCode()
    val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

    android.os.Handler(Looper.getMainLooper()).postDelayed({
        identifyAllViews(viewMainContainer as ViewGroup, guid, changeOrientation) }, 400)
}