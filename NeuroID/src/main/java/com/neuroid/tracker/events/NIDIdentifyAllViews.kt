package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.forEach
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.getIdOrTag

fun identifyAllViews(viewParent: ViewGroup) {
    viewParent.forEach {
        registerComponent(it)
        registerListeners(it)
        if (it is ViewGroup) {
            identifyAllViews(it)
        }
    }
}

private fun registerComponent(view: View) {
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
                    tg = hashMapOf(
                        "tgs" to idName,
                        "etn" to INPUT,
                        "et" to et
                    ),
                    v = "S~C~~0",
                    ts = System.currentTimeMillis()
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