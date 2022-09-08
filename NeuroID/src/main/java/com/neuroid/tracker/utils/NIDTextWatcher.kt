package com.neuroid.tracker.utils

import android.text.Editable
import android.text.TextWatcher
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.extensions.getSHA256
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson

class NIDTextWatcher(
    private val idName: String
) : TextWatcher {

    private var lastSize = 0

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No operation
    }

    override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
        var typeEvent = ""
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (before == 0 && count - before > 1) {
            typeEvent = PASTE
        }

        if (typeEvent.isNotEmpty()) {
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = typeEvent,
                        ts = System.currentTimeMillis(),
                        tg = hashMapOf(
                            "attr" to getAttrJson(sequence.toString()),
                        ),
                        gyro = gyroData,
                        accel = accelData
                    )
                )
        }


    }

    override fun afterTextChanged(sequence: Editable?) {
        val ts = System.currentTimeMillis()
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()


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
                        hv = sequence?.toString()?.getSHA256()?.take(8),
                        gyro = gyroData,
                        accel = accelData
                    )
                )
        }
        lastSize = sequence?.length ?: 0
    }
}