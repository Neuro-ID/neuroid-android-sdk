package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.forEach
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
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
    }

    if (et.isNotEmpty()) {
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = REGISTER_TARGET,
                    et = view.javaClass.simpleName,
                    eid = idName,
                    tgs = idName,
                    en = idName,
                    v = "S~C~~0",
                    ts = System.currentTimeMillis(),
                    url = nameScreen
                ).getOwnJson())
    }
}

private fun registerListeners(view: View) {
    when (view) {
        is Spinner -> {
            view.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SELECT_CHANGE,
                                ts = System.currentTimeMillis()
                            ).getOwnJson())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //No Op
                }
            }
        }
    }
}