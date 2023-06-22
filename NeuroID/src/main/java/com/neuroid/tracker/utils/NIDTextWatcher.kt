package com.neuroid.tracker.utils

import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson


class NIDTextWatcher(
    private val idName: String,
    val className: String? = ""
) : TextWatcher {

    private var lastSize = 0

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No operation
    }

    override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
        /**
         * Potentially check for paste here
         */
//        NIDLog.d(
//            "NIDDebugEvent",
//            "**OnTextChange $idName - $className - $sequence - $start - $before - $count"
//        )


        // Check if the change is due to a paste operation
        val clipboard = NeuroID.getInstance()?.getApplicationContext()
            ?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val pastedText = clipData.getItemAt(0).text
            if (sequence.toString().contains(pastedText)) {
                // The change is likely due to a paste operation
                NIDLog.d(
                    "NID-a",
                    "**OnTextChange Clipboard Entry $idName - $pastedText - ${clipData.itemCount}"
                )
            }
        }
    }

    override fun afterTextChanged(sequence: Editable?) {
        val ts = System.currentTimeMillis()
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

//        NIDLog.d("NIDDebugEvent", "**AfterTextChange $idName - $className")
        NIDLog.d("NID-Activity", "after text ${sequence.toString()}")
        if (lastSize != sequence?.length) {
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = INPUT,
                        ts = ts,
                        tg = hashMapOf(
                            "attr" to getAttrJson(sequence.toString()),
                            "etn" to INPUT,
                            "et" to "text"
                        ),
                        tgs = idName,
                        v = "S~C~~${sequence?.length}",
                        hv = sequence?.toString()?.getSHA256withSalt()?.take(8),
                        gyro = gyroData,
                        accel = accelData
                    )
                )
        }
        lastSize = sequence?.length ?: 0
    }
}