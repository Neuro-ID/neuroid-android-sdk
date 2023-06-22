package com.neuroid.tracker.events

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.forEach
import com.neuroid.tracker.callbacks.NIDTextContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDLongPressContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.getIdOrTag
import com.neuroid.tracker.utils.getParents
import org.json.JSONArray
import org.json.JSONObject

fun identifyView(
    view: View,
    guid: String,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true
) {
    when (view) {
        is ViewGroup -> identifyAllViews(view, guid, registerTarget, registerListeners)
        else -> {
            if (registerTarget) {
                registerComponent(view, guid)
            }
            if (registerListeners) {
                registerListeners(view)
            }
        }
    }
}

fun identifyAllViews(
    viewParent: ViewGroup,
    guid: String,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true
) {
    NIDLog.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

    viewParent.forEach {
        if (it is ViewGroup) {
            identifyAllViews(it, guid, registerTarget, registerListeners)
            it.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
//                    NIDLog.d(
//                        "NID-Activity",
//                        "CHILD VIEW ${
//                            child?.getIdOrTag().orEmpty()
//                        }  ${
//                            child?.javaClass?.simpleName
//                        } from ${parent.getIdOrTag()} ${parent?.javaClass?.simpleName} -"
//                    )
                    NIDLog.d(
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
//                    NIDLog.d(
//                        "NID-Activity",
//                        "CHILD VIEW REMOVED ${
//                            child?.getIdOrTag().orEmpty()
//                        } from ${parent.getIdOrTag()} ${parent?.javaClass?.simpleName}"
//                    )
                    NIDLog.d(
                        "NIDDebug ViewListener",
                        "ViewRemoved: ${child?.getIdOrTag().orEmpty()}"
                    )
                }
            })
        } else {
            identifyView(it, guid, registerTarget, registerListeners)
        }
    }
}

fun registerComponent(view: View, guid: String, rts: String? = null) {
    NIDLog.d(
        "NIDDebug registeredComponent",
        "view: ${view::class} java: ${view.javaClass.simpleName}"
    )

    val idName = view.getIdOrTag()
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()
    var et = ""

    when (view) {

        is EditText -> {
            et = "Edittext"
        }
//        is CheckBox, is AppCompatCheckBox -> {
//            et = "CheckBox"
//        }
//        is RadioButton -> {
//            et = "RadioButton"
//        }
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

    NIDLog.d("NIDDebug et at registerComponent", "$et")

    // early exit if not supported target type
    if (et.isEmpty()) {
        return
    }

//    NIDLog.d(
//        "NID-Activity",
//        "Actually Target Registered $et - ${view.javaClass.simpleName} - ${view::class} - ${view.getIdOrTag()}"
//    )
    val pathFrag = if (NIDServiceTracker.screenFragName.isEmpty()) {
        ""
    } else {
        "/${NIDServiceTracker.screenFragName}"
    }
    val urlView = ANDROID_URI + NIDServiceTracker.screenActivityName + "$pathFrag/" + idName

    val idJson = JSONObject().put("n", "guid").put("v", guid)
    val classJson = JSONObject().put("n", "screenHierarchy")
        .put("v", "${view.getParents()}${NIDServiceTracker.screenName}")
    val attrJson = JSONArray().put(idJson).put(classJson)

    getDataStoreInstance()
        .saveEvent(
            NIDEventModel(
                type = REGISTER_TARGET,
                attrs = attrJson,
                tg = mapOf("attr" to attrJson),
                et = et + "::" + view.javaClass.simpleName,
                etn = "INPUT",
                ec = NIDServiceTracker.screenName,
                eid = idName,
                tgs = idName,
                en = idName,
                v = "S~C~~0",
                hv = "",
                ts = System.currentTimeMillis(),
                url = urlView,
                gyro = gyroData,
                accel = accelData,
                rts = rts
            )
        )
}

private fun registerListeners(view: View) {
    val idName = view.getIdOrTag()
    val simpleClassName = view.javaClass.simpleName
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    // EditText is a parent class to multiple components
    if (view is EditText) {
        NIDLog.d(
            "NID-Activity",
            "EditText Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}"
        )
        // add Text Change watcher
        val textWatcher = NIDTextWatcher(idName, simpleClassName)
        view.addTextChangedListener(textWatcher)

        // add original action menu watcher
        val actionCallback = view.customSelectionActionModeCallback
        if (actionCallback !is NIDTextContextMenuCallbacks) {
            view.customSelectionActionModeCallback = NIDTextContextMenuCallbacks(actionCallback)
        }

        // if later api version, add additional action menu watcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addExtraActionMenuListener(view)
        }
    }

    // additional subclasses to be captured
    when (view) {
        is AbsSpinner -> {
//            NIDLog.d(
//                "NID-Activity",
//                "GENERIC SPINNER Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}"
//            )
            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    simpleClassName,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }
        is Spinner -> {
//            NIDLog.d(
//                "NID-Activity",
//                "SPINNER Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}"
//            )

            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    simpleClassName,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }
        is AutoCompleteTextView -> {
            NIDLog.d(
                "NID-Activity",
                "AUTOCOMPLETE Listener ${view.javaClass.simpleName} - ${view::class} - ${view.getIdOrTag()}"
            )
            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    simpleClassName,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }
    }
}

private fun addSelectOnSelect(
    idName: String,
    lastSelectListener: AdapterView.OnItemSelectedListener?,
    simpleClassName: String,
    gyroData: NIDSensorModel?,
    accelData: NIDSensorModel?,
): AdapterView.OnItemSelectedListener {
    return object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            adapter: AdapterView<*>?,
            viewList: View?,
            position: Int,
            p3: Long
        ) {
            lastSelectListener?.onItemSelected(adapter, viewList, position, p3)

//            NIDLog.d(
//                "NID-Activity",
//                "Select Selected $idName - $simpleClassName $position $p3 $viewList"
//            )
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = SELECT_CHANGE,
                        tg = hashMapOf(
                            "etn" to simpleClassName,
                            "tgs" to idName,
                            "sender" to simpleClassName
                        ),
                        tgs = idName,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        v = "$position"
                    )
                )
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            lastSelectListener?.onNothingSelected(p0)
        }
    }
}

private fun addSelectOnClickListener(
    idName: String,
    lastClickListener: AdapterView.OnItemClickListener?,
    simpleClassName: String,
    gyroData: NIDSensorModel?,
    accelData: NIDSensorModel?,
): AdapterView.OnItemClickListener {
    return AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
        lastClickListener?.onItemClick(adapter, viewList, position, p3)

//        NIDLog.d(
//            "NID-Activity",
//            "Select CLICK $idName - $simpleClassName $position $p3 $viewList"
//        )
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = SELECT_CHANGE,
                    tg = hashMapOf(
                        "etn" to "INPUT",
                        "et" to "text"
                    ),
                    tgs = idName,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    v = "$position"
                )
            )
    }
}


@RequiresApi(Build.VERSION_CODES.M)
private fun addExtraActionMenuListener(view: EditText) {
    val actionInsertionCallback = view.customInsertionActionModeCallback
    if (actionInsertionCallback !is NIDLongPressContextMenuCallbacks) {
        view.customInsertionActionModeCallback =
            NIDLongPressContextMenuCallbacks(actionInsertionCallback)
    }
}