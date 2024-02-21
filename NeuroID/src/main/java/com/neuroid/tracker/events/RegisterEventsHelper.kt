package com.neuroid.tracker.events

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.callbacks.NIDGlobalEventCallback
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import java.util.UUID

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
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = "",
) {
    // DEBUG are we actually fetching all view containers
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    ) as ViewGroup

    val hashCodeAct = activity.hashCode()
    val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

    handleIdentifyAllViews {
        identifyAllViews(
            viewMainContainer,
            guid,
            logger,
            storeManager,
            registerTarget,
            registerListeners,
            activityOrFragment,
            parent
        )
    }
}