package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.forEach
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.getIdOrTag

fun identifyAllViews(viewParent: ViewGroup, nameScreen: String) {
    viewParent.forEach {
        registerComponent(it, nameScreen)
        registerListeners(it)
        if (it is ViewGroup) {
            identifyAllViews(it, nameScreen)
        }
    }
}

private fun registerComponent(view: View, nameScreen: String) {
    val idName = view.getIdOrTag()
    var et = ""

    when(view) {
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
        else -> {
            NIDLog.d("NeuroID", "--------------- Other view: $view")
        }
    }

    if (et.isNotEmpty()) {
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = REGISTER_TARGET,
                    et = view.javaClass.simpleName,
                    etn = "INPUT",
                    ec = nameScreen,
                    eid = idName,
                    tgs = idName,
                    en = idName,
                    v = "S~C~~0",
                    ts = System.currentTimeMillis(),
                    url = nameScreen
                ))
    }
}

private fun registerListeners(view: View) {
    val idName = view.getIdOrTag()

    when (view) {
        is Spinner -> {
            val lastListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapter: AdapterView<*>?, viewList: View?, position: Int, p3: Long) {
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
                                ts = System.currentTimeMillis()
                            ))
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    lastListener?.onNothingSelected(p0)
                }
            }
        }
        is AutoCompleteTextView -> {
            val lastListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener = AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
                lastListener.onItemClick(adapter, viewList, position, p3)
                getDataStoreInstance()
                    .saveEvent(
                        NIDEventModel(
                            type = SELECT_CHANGE,
                            tg = hashMapOf(
                                "tgs" to idName,
                                "etn" to "INPUT",
                                "et" to "text"
                            ),
                            ts = System.currentTimeMillis()
                        ))
            }
        }
    }
}