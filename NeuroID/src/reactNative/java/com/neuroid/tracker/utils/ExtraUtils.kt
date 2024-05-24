package com.neuroid.tracker.utils

import android.os.Looper
import android.view.View
import androidx.core.view.children
import com.facebook.react.views.image.ReactImageView
import com.facebook.react.views.text.ReactTextView
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup
import com.neuroid.tracker.events.ComponentValuesResult
import com.neuroid.tracker.events.detectBasicAndroidViewType
import com.neuroid.tracker.events.isCommonAndroidComponent
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.extensions.getSHA256withSalt

// In RN users call registerPageTargets so we don't
//    want to run this code and double register
internal fun registrationHelpers(r: Runnable)  {
}

fun isCommonReactNativeComponent(view: View): ComponentValuesResult {
    // vars that can change based on the element
    var idName = view.getIdOrTag()
    var et = ""
    var v = "S~C~~0"
    val metaData = mutableMapOf<String, Any>()

    when (view) {
        is ReactEditText -> {
            et = "ReactEditText"
            v = "S~C~~${view.text?.length}"
        }

        is ReactViewGroup -> {
            if (view.hasOnClickListeners() && view.children.count() == 1) {
                val child = view.children.firstOrNull()
                if (child is ReactTextView) {
                    et = "ReactButton::${child.javaClass.simpleName}"
                    idName = "$idName-${
                        child?.getIdOrTag()
                    }-${child?.text.toString().getSHA256withSalt()?.take(8)}"
                } else if (child is ReactImageView) {
                    et = "ReactButton::${child.javaClass.simpleName}"
                    idName = "$idName-${
                        child?.getIdOrTag()
                    }"
                }
            }
        }
    }

    return ComponentValuesResult(
        idName,
        et,
        v,
        metaData,
    )
}

fun verifyComponentType(view: View): ComponentValuesResult {
    var (idName, et, v, metaData) = isCommonAndroidComponent(view)

    // check if its empty, then check if its a RN element
    if (et.isEmpty()) {
        var (rnIdName, rnEt, rnV, rnMetaData) = isCommonReactNativeComponent(view)

        idName = rnIdName
        et = rnEt
        v = rnV
        metaData = rnMetaData
    }

    return ComponentValuesResult(
        idName,
        et,
        v,
        metaData,
    )
}

// run the function after a 300ms delay in react native
internal fun handleIdentifyAllViews(r: Runnable)  {
    android.os.Handler(Looper.getMainLooper()).postDelayed({
        r.run()
    }, 300)
}

internal fun detectViewType(currentView: View?): Int {
    var typeOfView =
        when (currentView) {
            is ReactEditText,
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
        typeOfView = detectBasicAndroidViewType(currentView)
    }

    return typeOfView
}

internal fun getEtnSenderName(currentView: View?): String  {
    return currentView?.getIdOrTag() ?: "main_view"
}
