package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.ToggleButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.forEach
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.extensions.getParents
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper

fun identifyAllViews(
    viewParent: ViewGroup,
    guid: String,
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = ""
) {
    logger.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

    viewParent.forEach {
        when (it) {
            is ViewGroup -> {
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
//                         identifyView(view, guid, registerTarget, registerListeners)
                        }
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        logger.d(
                            "NIDDebug ViewListener",
                            "ViewRemoved: ${child?.getIdOrTag().orEmpty()}"
                        )
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


fun createAtrrJSON(
    logger: NIDLogWrapper,
    view: View,
    guid: String,
    activityOrFragment: String = "",
    parent: String = "",
    metaData: Map<String, Any>
): List<Map<String, Any>> {
    val idJson = mapOf<String, Any>(
        "n" to "guid",
        "v" to guid
    )

    val classJson = mapOf<String, Any>(
        "n" to "screenHierarchy",
        "v" to "${view.getParents(logger)}${NeuroID.screenName}"
    )

    val parentData =
        mapOf<String, Any>(
            "parentClass" to parent,
            "component" to activityOrFragment
        )

    return listOf(
        idJson,
        classJson,
        parentData,
        metaData
    )
}

fun registerFinalComponent(
    logger: NIDLogWrapper,
    storeManager: NIDDataStoreManager,
    rts: String? = null,
    idName: String,
    et: String,
    v: String,
    simpleName: String,
    attrJson: List<Map<String, Any>>
) {
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    val pathFrag = if (NeuroID.screenFragName.isEmpty()) {
        ""
    } else {
        "/${NeuroID.screenFragName}"
    }

    val urlView = ANDROID_URI + NeuroID.screenActivityName + "$pathFrag/" + idName

    logger.d("NID test output", "etn: INPUT, et: $simpleName, eid: $idName, v:$v")

    storeManager
        .saveEvent(
            NIDEventModel(
                type = REGISTER_TARGET,
                attrs = attrJson,
                tg = mapOf("attr" to attrJson),
                et = "$et::$simpleName",
                etn = "INPUT",
                ec = NeuroID.screenName,
                eid = idName,
                tgs = idName,
                en = idName,
                v = v,
                hv = v.getSHA256withSalt().take(8),
                ts = System.currentTimeMillis(),
                url = urlView,
                gyro = gyroData,
                accel = accelData,
                rts = rts
            )
        )
}

data class ComponentValuesResult(
    val idName: String,
    val et: String,
    val v: String,
    val metaData: Map<String, Any>
)

fun isCommonAndroidComponent(view: View): ComponentValuesResult {
    // vars that can change based on the element
    var idName = view.getIdOrTag()
    var et = ""
    var v = "S~C~~0"
    val metaData = mutableMapOf<String, Any>()

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
            metaData.put("id", "$idName")

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

        is Switch, is SwitchCompat -> {
            et = "Switch"
        }

        is Button, is ImageButton -> {
            et = "Button"
        }

        is SeekBar -> {
            et = "SeekBar"
        }

        is Spinner -> {
            et = "Spinner"
        }

        is RatingBar -> {
            et = "RatingBar"
        }
    }

    return ComponentValuesResult(
        idName,
        et,
        v,
        metaData
    )
}