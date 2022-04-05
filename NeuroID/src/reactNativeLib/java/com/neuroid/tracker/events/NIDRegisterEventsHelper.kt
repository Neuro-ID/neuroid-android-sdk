package com.neuroid.tracker.events

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.callbacks.*
import com.neuroid.tracker.service.NIDServiceTracker
import java.util.*

fun registerLaterLifecycleFragments(activity: Activity) {
    val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
    fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)

    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDOnLoadReactRootListener(viewMainContainer, activity))
}

fun registerViewsEventsForActivity(activity: Activity, saveScreenName: Boolean = false) {
    if (saveScreenName) {
        NIDServiceTracker.screenName = activity::class.java.name
    }

    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))

    val callBack = activity.window.callback
    val touchManager = NIDTouchEventManager(viewMainContainer as ViewGroup)
    activity.window.callback = NIDWindowCallback(callBack, touchManager)
}

fun registerViewsEventsForFragment(activity: Activity) {
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
        identifyAllViews(viewMainContainer, activity::class.java.simpleName, guid) }, 400)
}

fun unRegisterListenerFromActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.removeOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))
}