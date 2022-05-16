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
): TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No op
    }

    override fun onTextChanged(sequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        val attrs = listOf(
            NIDAttrItem("v", "S~C~~${sequence?.length ?: 0}").getJson(),
            NIDAttrItem("hash", sequence.toString().getSHA256().take(8)).getJson()
        )

        val ts = System.currentTimeMillis()

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

    override fun afterTextChanged(p0: Editable?) {
        // No op
    }
}