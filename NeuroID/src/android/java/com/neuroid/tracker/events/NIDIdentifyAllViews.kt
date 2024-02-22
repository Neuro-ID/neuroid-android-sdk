package com.neuroid.tracker.events

import android.view.View

fun verifyComponentType(view: View): ComponentValuesResult {
    return isCommonAndroidComponent(view)
}
