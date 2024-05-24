package com.neuroid.tracker.utils

import android.view.View
import com.neuroid.tracker.events.ComponentValuesResult
import com.neuroid.tracker.events.detectBasicAndroidViewType
import com.neuroid.tracker.events.isCommonAndroidComponent

// Native Android needs to register the activities
internal fun registrationHelpers(r: Runnable)  {
    r.run()
}

fun verifyComponentType(view: View): ComponentValuesResult {
    return isCommonAndroidComponent(view)
}

// run the function immediately in native android
internal fun handleIdentifyAllViews(r: Runnable)  {
    r.run()
}

internal fun detectViewType(currentView: View?): Int {
    return detectBasicAndroidViewType(currentView)
}

internal fun getEtnSenderName(currentView: View?): String  {
    return currentView?.javaClass?.simpleName.orEmpty()
}
