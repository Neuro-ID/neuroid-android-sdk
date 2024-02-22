package com.neuroid.tracker.events

import android.view.View
import android.widget.*
import androidx.appcompat.widget.SwitchCompat

internal fun detectViewType(currentView:View?):Int {
    return detectBasicAndroidViewType(currentView)
}

internal fun getEtnSenderName(currentView:View?): String{
    return currentView?.javaClass?.simpleName.orEmpty()
}