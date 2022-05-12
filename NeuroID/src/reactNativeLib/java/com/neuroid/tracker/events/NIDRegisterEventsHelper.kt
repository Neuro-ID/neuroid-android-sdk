package com.neuroid.tracker.events

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.callbacks.*
import com.neuroid.tracker.service.NIDServiceTracker
import java.util.*

fun registerLaterLifecycleFragments(activity: Activity) {
    NIDServiceTracker.screenName = activity::class.java.name

    val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
    fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)

    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(NIDOnLoadReactRootListener(viewMainContainer, activity))
}

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

    identifyAllViews(viewMainContainer, guid)
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

    identifyAllViews(viewMainContainer, guid)
}

fun unRegisterListenerFromActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.removeOnGlobalFocusChangeListener(NIDFocusChangeListener())
    viewMainContainer.viewTreeObserver.removeOnGlobalLayoutListener(NIDLayoutChangeListener(viewMainContainer))
}