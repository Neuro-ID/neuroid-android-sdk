package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.core.view.forEach
import com.facebook.react.views.image.ReactImageView
import com.facebook.react.views.text.ReactTextView
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup
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

fun identifyAllViews(
    viewParent: ViewGroup,
    guid: String,
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    registerTarget: Boolean,
    registerListeners: Boolean,
    activityOrFragment: String = "",
    parent: String = ""
) {
    logger.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

    viewParent.forEach {
        when (it) {
            is ViewGroup -> {

                if (it.hasOnClickListeners()) {
                    val firstChild = it.children.firstOrNull()
                    if (firstChild != null) {
                        registerElement(
                            it,
                            guid,
                            logger,
                            storeManager,
                            registerTarget,
                            registerListeners,
                            activityOrFragment = activityOrFragment,
                            parent = parent
                        )
                    }
                }

                identifyAllViews(
                    it,
                    guid,
                    logger,
                    storeManager,
                    registerTarget,
                    registerListeners,
                    activityOrFragment,
                    parent
                )

                it.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {

                    override fun onChildViewAdded(parent: View?, child: View?) {
                        logger.d(
                            "NIDDebug ChildViewAdded",
                            "ViewAdded: ${child?.getIdOrTag().orEmpty()}"
                        )

                        child?.let { view ->
                            // This is double registering targets and registering listeners before the correct
                            //  lifecycle event which is causing a replay of text input events to occur
//                            identifyView(view, guid, registerTarget, registerListeners)
                        }
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        logger.i("ViewListener", "ViewRemoved: ${child?.getIdOrTag().orEmpty()}")
                    }
                })
            }

            else -> {
                identifyView(
                    it,
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

        // exception groups that should be registered:
        when (it) {
            is RadioGroup -> {
                identifyView(
                    it,
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
    }
}


fun identifyView(
    view: View,
    guid: String,
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = "",
) {
    when (view) {
        is ViewGroup -> identifyAllViews(
            view,
            guid,
            logger,
            storeManager,
            registerTarget,
            registerListeners,
            activityOrFragment = activityOrFragment,
            parent = parent
        )

        else -> {
            registerElement(
                view,
                guid,
                logger,
                storeManager,
                registerTarget,
                registerListeners,
                activityOrFragment = activityOrFragment,
                parent = parent
            )
        }
    }

    // exception groups that should be registered:
    when (view) {
        is RadioGroup -> {
            registerElement(
                view,
                guid,
                logger,
                storeManager,
                registerTarget,
                registerListeners,
                activityOrFragment = activityOrFragment,
                parent = parent
            )
        }
    }
}

fun registerElement(
    view: View,
    guid: String,
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = "",
) {
    if (registerTarget) {
        registerComponent(
            view,
            guid,
            logger,
            storeManager,
            activityOrFragment = activityOrFragment,
            parent = parent
        )
    }
    if (registerListeners) {
        registerListeners(view, logger)
    }
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
    logger.d(
        "NIDDebug registeredComponent",
        "view: ${view::class} java: ${view.javaClass.simpleName}"
    )

    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()
    var idName = view.getIdOrTag()
    var et = ""
    var v = "S~C~~0"
    val metaData = JSONObject()

    when (view) {
        is EditText -> {
            et = "Edittext"
            v = "S~C~~${view.text.length}"
        }

        is CheckBox -> {
            et = "CheckBox"
        }

        is RadioButton -> {
            et = "RadioButton"
            v = "${view.isChecked}"

            metaData.put("type", "radioButton")
            metaData.put("id", "${view.getIdOrTag()}")

            // go up to 3 parents in case a RadioGroup is not the direct parent
            var rParent = view.parent;
            repeat(3) { index ->
                if (rParent is RadioGroup) {
                    val p = rParent as RadioGroup
                    metaData.put("rGroupId", "${p.getIdOrTag()}")
                    return@repeat
                } else {
                    rParent = rParent.parent
                }
            }
        }

        is RadioGroup -> {
            et = "RadioGroup"
            v = "${view.checkedRadioButtonId}"

        }

        is ToggleButton -> {
            et = "ToggleButton"
        }

        is Switch -> {
            et = "Switch"
        }

        is Button -> {
            et = "Button"
        }

        is SeekBar -> {
            et = "SeekBar"
        }

        is Spinner -> {
            et = "Spinner"
        }

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


    logger.d("NIDDebug et at registerComponent", "${et}")

    // early exit if not supported target type
    if (et.isEmpty()) {
        return
    }

    val pathFrag = if (NIDServiceTracker.screenFragName.isEmpty()) {
        ""
    } else {
        "/${NIDServiceTracker.screenFragName}"
    }
    val urlView = ANDROID_URI + NIDServiceTracker.screenActivityName + "$pathFrag/" + idName

    val idJson = JSONObject().put("n", "guid").put("v", guid)
    val classJson = JSONObject().put("n", "screenHierarchy")
        .put("v", "${view.getParents(logger)}${NIDServiceTracker.screenName}")
    val parentData =
        JSONObject().put("parentClass", "$parent").put("component", "$activityOrFragment")

    val attrJson = JSONArray().put(idJson).put(classJson).put(parentData).put(metaData)

    storeManager
        .saveEvent(
            NIDEventModel(
                type = REGISTER_TARGET,
                attrs = attrJson,
                et = et + "::" + view.javaClass.simpleName,
                etn = "INPUT",
                ec = NIDServiceTracker.screenName,
                eid = idName,
                tgs = idName,
                en = idName,
                v = v,
                hv = v.getSHA256withSalt()?.take(8),
                ts = System.currentTimeMillis(),
                url = urlView,
                gyro = gyroData,
                accel = accelData,
                rts = rts
            )
        )
}