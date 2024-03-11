package com.neuroid.tracker.events

import android.view.View
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
import com.neuroid.tracker.extensions.getIdOrTag

data class ComponentValuesResult(
    val idName: String,
    val et: String,
    val v: String,
    val metaData: Map<String, Any>,
)

fun isCommonAndroidComponent(view: View): ComponentValuesResult {
    // vars that can change based on the element
    val idName = view.getIdOrTag()
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
            metaData.put("id", idName)

            // go up to 3 parents in case a RadioGroup is not the direct parent
            var rParent = view.parent
            repeat(3) { index ->
                if (rParent is RadioGroup) {
                    val p = rParent as RadioGroup
                    metaData.put("rGroupId", p.getIdOrTag())
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
        metaData,
    )
}
