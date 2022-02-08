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
        val ts = System.currentTimeMillis()
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = KEY_UP,
                    ts = ts,
                    tg = hashMapOf(
                        "tgs" to idName,
                        "etn" to INPUT,
                        "et" to "text"
                    )
                ).getOwnJson()
            )

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                    tg = hashMapOf(
                        "tgs" to idName,
                        "etn" to INPUT,
                        "et" to "text"
                    )
                ).getOwnJson()
            )
    }

    override fun afterTextChanged(p0: Editable?) {
        // No op
    }
}