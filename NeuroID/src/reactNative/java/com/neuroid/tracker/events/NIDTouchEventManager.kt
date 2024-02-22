package com.neuroid.tracker.events

import android.view.View
import com.neuroid.tracker.extensions.getIdOrTag

import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup

internal fun detectViewType(currentView:View?):Int {
    var typeOfView = when (currentView) {
        is ReactEditText
        -> 1
        is ReactViewGroup -> {
            if (currentView.isFocusable) {
                2
            } else {
                0
            }
        }
        else -> 0
    }

    if (typeOfView == 0) {
        typeOfView  = detectBasicAndroidViewType(currentView)
    }

    return typeOfView
}

internal fun getEtnSenderName(currentView:View?): String{
    return currentView?.getIdOrTag() ?: "main_view"
}