package com.neuroid.tracker.utils

import android.content.ClipData
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
import org.json.JSONArray
import org.json.JSONObject


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
            var pastedText = ""
            try {
                pastedText = clipData.getItemAt(0).text.toString()
            } catch (e: Exception) {
                e.message?.let {
                    NIDLog.e("NID-Activity",it)
                }
            }
            val pasteCount = pastedText.length
            if (sequence.toString().contains(pastedText) && (pasteCount == count)) {
                // The change is likely due to a paste operation
                val ts = System.currentTimeMillis()
                val gyroData = NIDSensorHelper.getGyroscopeInfo()
                val accelData = NIDSensorHelper.getAccelerometerInfo()

                val metadataObj = JSONObject()
                metadataObj.put("clipboardText", "S~C~~${pastedText.length}")

                val attrJSON = JSONArray().put(metadataObj)

                getDataStoreInstance()
                    .saveEvent(
                        NIDEventModel(
                            type = PASTE,
                            ts = ts,
                            tg = hashMapOf(
                                "attr" to getAttrJson(sequence.toString()),
                                "et" to "text"
                            ),
                            tgs = idName,
                            v = "S~C~~${sequence?.length}",
                            hv = sequence?.toString()?.getSHA256withSalt()?.take(8),
                            gyro = gyroData,
                            accel = accelData,
                            attrs = attrJSON
                        )
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
}