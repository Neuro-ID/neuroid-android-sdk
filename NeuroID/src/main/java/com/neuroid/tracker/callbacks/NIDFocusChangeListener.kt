package com.neuroid.tracker.callbacks

import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.TEXT_CHANGE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.getIdOrTag

class NIDFocusChangeListener: ViewTreeObserver.OnGlobalFocusChangeListener {
    private var lastEditText: EditText? = null

    override fun onGlobalFocusChanged(oldView: View?, newView: View?) {
        val ts = System.currentTimeMillis()
        if (newView != null) {
            val idName = newView.getIdOrTag()

            if (newView is EditText) {
                getDataStoreInstance()
                    .saveEvent(
                        NIDEventModel(
                            type = FOCUS,
                            ts = ts,
                            tg = hashMapOf(
                                "tgs" to idName,
                                "etn" to INPUT,
                                "et" to "text"
                            )
                        ).getOwnJson()
                    )

                val textWatcher = NIDTextWatcher(idName)
                newView.removeTextChangedListener(textWatcher)
                newView.addTextChangedListener(textWatcher)

                val actionCallback = newView.customSelectionActionModeCallback
                if (actionCallback !is NIDContextMenuCallbacks) {
                    newView.customSelectionActionModeCallback = NIDContextMenuCallbacks(newView.context, actionCallback)
                }

                lastEditText = if (lastEditText == null) {
                    newView
                } else {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TEXT_CHANGE,
                                tg = hashMapOf(
                                    "tgs" to lastEditText?.getIdOrTag().orEmpty(),
                                    "etn" to INPUT,
                                    "et" to "text"
                                ),
                                v = "S~C~~${lastEditText?.text.toString().length}",
                                ts = ts
                            ).getOwnJson()
                        )

                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = BLUR,
                                tgs = lastEditText?.getIdOrTag(),
                                ts = ts
                            ).getOwnJson()
                        )
                    null
                }
            }
        }
    }
}