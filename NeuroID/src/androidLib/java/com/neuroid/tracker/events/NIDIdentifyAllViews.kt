package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.forEach
import com.neuroid.tracker.callbacks.NIDContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.getIdOrTag
import com.neuroid.tracker.utils.getParents

fun identifyAllViews(
    viewParent: ViewGroup,
    guid: String,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true
) {
    viewParent.forEach {
        if (registerTarget) {
            registerComponent(it, guid)
        }
        if (registerListeners) {
            registerListeners(it)
        }
        if (it is ViewGroup) {
            identifyAllViews(it, guid, registerTarget, registerListeners)
        }
    }
}

private fun registerComponent(view: View, guid: String) {
    val idName = view.getIdOrTag()
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()
    var et = ""

    when (view) {
        is EditText -> {
            et = "Edittext"
        }
        is CheckBox, is AppCompatCheckBox -> {
            et = "CheckBox"
        }
        is RadioButton -> {
            et = "RadioButton"
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

    if (et.isNotEmpty()) {
        val pathFrag = if (NIDServiceTracker.screenFragName.isEmpty()) {
            ""
        } else {
            "/${NIDServiceTracker.screenFragName}"
        }
        val urlView = ANDROID_URI + NIDServiceTracker.screenActivityName + "$pathFrag/" + idName

        val attrs = "{" +
                "\"guid\":\"$guid\"," +
                "\"screenHierarchy\":\"${view.getParents()}${NIDServiceTracker.screenName}\"" +
                "}"

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = REGISTER_TARGET,
                    et = et + "::" + view.javaClass.simpleName,
                    etn = "INPUT",
                    ec = NIDServiceTracker.screenName,
                    eid = idName,
                    tgs = idName,
                    en = idName,
                    v = "S~C~~0",
                    ts = System.currentTimeMillis(),
                    tg = hashMapOf(
                        "attr" to attrs
                    ),
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

    if (view is EditText) {
        val textWatcher = NIDTextWatcher(idName)
        view.addTextChangedListener(textWatcher)

        val actionCallback = view.customSelectionActionModeCallback
        if (actionCallback !is NIDContextMenuCallbacks) {
            view.customSelectionActionModeCallback = NIDContextMenuCallbacks(actionCallback)
        }
    }

    when (view) {
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
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SELECT_CHANGE,
                                tg = hashMapOf(
                                    "tgs" to idName,
                                    "etn" to "INPUT",
                                    "et" to "text"
                                ),
                                ts = System.currentTimeMillis(),
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    lastListener?.onNothingSelected(p0)
                }
            }
        }
        is AutoCompleteTextView -> {
            val lastListener: AdapterView.OnItemClickListener? = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
                    lastListener?.onItemClick(adapter, viewList, position, p3)
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SELECT_CHANGE,
                                tg = hashMapOf(
                                    "tgs" to idName,
                                    "etn" to "INPUT",
                                    "et" to "text"
                                ),
                                ts = System.currentTimeMillis(),
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }
        }
    }
}