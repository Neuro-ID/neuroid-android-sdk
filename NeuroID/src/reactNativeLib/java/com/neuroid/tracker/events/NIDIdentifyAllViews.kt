package com.neuroid.tracker.events

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.view.forEach
import com.facebook.react.views.text.ReactTextView
import com.neuroid.tracker.callbacks.NIDContextMenuCallbacks
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.getIdOrTag
import com.neuroid.tracker.utils.getParents
import com.neuroid.tracker.utils.NIDLog
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
    registerTarget: Boolean,
    registerListeners: Boolean
) {
    NIDLog.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

    viewParent.forEach {
        if (registerTarget) {
            registerComponent(it, guid)
        }
        if (registerListeners) {
            registerListeners(it)
        }
        if (it is ViewGroup) {
            identifyAllViews(it, guid, registerTarget, registerListeners)
            it.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {

                override fun onChildViewAdded(parent: View?, child: View?) {
                    NIDLog.d("NIDDebug ChildViewAdded", "ViewAdded: ${child?.getIdOrTag().orEmpty()}")

                    child?.let { view ->
                        identifyView(view, guid, registerTarget, registerListeners)
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {
                    Log.i("ViewListener", "ViewRemoved: ${child?.getIdOrTag().orEmpty()}")
                }
            })
        }
    }
}

private fun registerComponent(view: View, guid: String) {
    val idName = view.getIdOrTag()
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()
    var et = ""

    NIDLog.d("NIDDebug registeredComponent", "view: ${view::class} java: ${view.javaClass.simpleName}")

    when (view) {
        is EditText -> {
            et = "Edittext"
        }
        is CheckBox -> {
            et = "CheckBox"
        }
        is RadioButton -> {
            et = "RadioButton"
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
        }
        is ReactViewGroup -> {
            if (view.hasOnClickListeners() && view.children.count() == 1 && view.children.firstOrNull() is ReactTextView) {
                et = "ReactButton"
            }
        }
    }


    NIDLog.d("NIDDebug et at registerComponent", "${et}")

    if (et.isNotEmpty()) {
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
                    et = et + "::" + view.javaClass.simpleName,
                    etn = "INPUT",
                    ec = NIDServiceTracker.screenName,
                    eid = idName,
                    tgs = idName,
                    en = idName,
                    v = "S~C~~0",
                    ts = System.currentTimeMillis(),
                    url = urlView,
                    gyro = gyroData,
                    accel = accelData
                )
            )
    }
}

private fun registerListeners(view: View) {
    val idName = view.getIdOrTag()
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    when (view) {
        is EditText -> {
            val actionCallback = view.customSelectionActionModeCallback
            if (actionCallback !is NIDContextMenuCallbacks) {
                view.customSelectionActionModeCallback = NIDContextMenuCallbacks(actionCallback)
                val textWatcher = NIDTextWatcher(idName)
                view.addTextChangedListener(textWatcher)
            }
        }
        is Spinner -> {
            val lastListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapter: AdapterView<*>?,
                    viewList: View?,
                    position: Int,
                    p3: Long
                ) {
                    lastListener?.onItemSelected(adapter, viewList, position, p3)
                    /*getDataStoreInstance()
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
                                accel = accelData
                            ))*/
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    lastListener?.onNothingSelected(p0)
                }
            }
        }
        is AutoCompleteTextView -> {
            val lastListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
                    lastListener.onItemClick(adapter, viewList, position, p3)
                    /*getDataStoreInstance()
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
                                accel = accelData
                            ))*/
                }
        }

    }
}