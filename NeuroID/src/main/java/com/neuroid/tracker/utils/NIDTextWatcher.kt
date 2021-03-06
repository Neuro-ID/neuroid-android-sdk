package com.neuroid.tracker.utils

import android.text.Editable
import android.text.TextWatcher
import com.neuroid.tracker.events.*
import com.neuroid.tracker.extensions.getSHA256
import com.neuroid.tracker.models.NIDAttrItem
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDTextWatcher(
    private val idName: String
) : TextWatcher {

    private var lastSize = 0

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No operation
    }

    override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
        var typeEvent = ""

        if (before == 0 && count - before > 1) {
            typeEvent = PASTE
        }

        if (typeEvent.isNotEmpty()) {
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = typeEvent,
                        ts = System.currentTimeMillis(),
                    )
                )
        }


    }

    override fun afterTextChanged(sequence: Editable?) {
        val ts = System.currentTimeMillis()
        val attrs = listOf(
            NIDAttrItem("v", "S~C~~${sequence?.length ?: 0}").getJson(),
            NIDAttrItem("hash", sequence.toString().getSHA256().take(8)).getJson()
        )

        if (lastSize != sequence?.length) {
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = INPUT,
                        ts = ts,
                        tg = hashMapOf(
                            "attr" to attrs.joinToString("|"),
                            "tgs" to idName,
                            "etn" to INPUT,
                            "et" to "text"
                        )
                    )
                )
        }
        lastSize = sequence?.length ?: 0
    }
}