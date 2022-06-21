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
    NIDServiceTracker.screenActivityName = activity::class.java.name

    val fragManager = (activity as? AppCompatActivity)?.supportFragmentManager
    fragManager?.registerFragmentLifecycleCallbacks(NIDFragmentCallbacks(), true)

    registerWindowListeners(activity)
}

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
    registerTarget: Boolean,
    registerListeners: Boolean
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