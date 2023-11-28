package com.neuroid.tracker.events

import android.view.View
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper

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

    val (idName, et, v, metaData) = isCommonAndroidComponent(view)

    logger.d("NIDDebug et at registerComponent", "$et")

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