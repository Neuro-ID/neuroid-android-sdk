package com.neuroid.tracker.events

import android.view.View
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.extensions.getParents
import org.json.JSONArray
import org.json.JSONObject
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper

import androidx.core.view.children
import com.facebook.react.views.image.ReactImageView
import com.facebook.react.views.text.ReactTextView
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup

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
                    idName = "${idName}-${
                        child?.getIdOrTag()
                    }-${child?.text.toString().getSHA256withSalt()?.take(8)}"

                } else if (child is ReactImageView) {
                    et = "ReactButton::${child.javaClass.simpleName}"
                    idName = "${idName}-${
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
        metaData
    )
}

fun registerComponent(
    view: View,
    guid: String,
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    rts: String? = null,
    activityOrFragment: String = "",
    parent: String = "",
) {
    val simpleName = view.javaClass.simpleName

    logger.d(
        "NIDDebug registeredComponent",
        "view: ${view::class} java: $simpleName"
    )

    var (idName, et, v, metaData) = isCommonAndroidComponent(view)

    logger.d("NIDDebug et at registerComponent", "$et")

    // check if its empty, then check if its a RN element
    if (et.isEmpty()) {
        var (rnIdName, rnEt, rnV, rnMetaData) = isCommonReactNativeComponent(view)

        idName = rnIdName
        et = rnEt
        v = rnV
        metaData = rnMetaData
    }

    // early exit if not supported target type
    if (et.isEmpty()) {
        return
    }

    val attrJson = createAtrrJSON(
        logger,
        view,
        guid,
        activityOrFragment,
        parent,
        metaData
    )

    registerFinalComponent(
        logger,
        storeManager,
        rts,
        idName,
        et,
        v,
        simpleName,
        attrJson
    )
}