package com.neuroid.tracker.utils

import android.text.Editable
import android.text.TextWatcher
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDTextWatcher(
    private val idName: String
): TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No op
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = KEY_UP,
                    ts = System.currentTimeMillis(),
                    tgs = hashMapOf(
                        "tgs" to idName,
                        "etn" to INPUT,
                        "et" to "text"
                    )
                ).getOwnJson()
            )
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = TEXT_CHANGE,
                    ts = System.currentTimeMillis(),
                    tgs = hashMapOf(
                        "tgs" to idName
                    )
                ).getOwnJson()
            )
    }

    override fun afterTextChanged(p0: Editable?) {
        // No op
    }
}