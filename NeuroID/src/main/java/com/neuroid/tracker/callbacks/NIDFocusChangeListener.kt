package com.neuroid.tracker.callbacks

import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.TEXT_CHANGE
import com.neuroid.tracker.extensions.getSHA256
import com.neuroid.tracker.models.NIDAttrItem
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
                                "tgs" to idName
                            )
                        )
                    )

                val textWatcher = NIDTextWatcher(idName)
                newView.removeTextChangedListener(textWatcher)
                newView.addTextChangedListener(textWatcher)

                val actionCallback = newView.customSelectionActionModeCallback
                if (actionCallback !is NIDContextMenuCallbacks) {
                    newView.customSelectionActionModeCallback = NIDContextMenuCallbacks(actionCallback)
                }

                lastEditText = if (lastEditText == null) {
                    newView
                } else {
                    val actualText = lastEditText?.text.toString()
                    val attrs = listOf(
                        NIDAttrItem("v", "S~C~~${actualText.length}").getJson(),
                        NIDAttrItem("hash", actualText.getSHA256().take(8)).getJson()
                    )

                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TEXT_CHANGE,
                                tg = hashMapOf(
                                    "attr" to attrs.joinToString("|"),
                                    "tgs" to lastEditText?.getIdOrTag().orEmpty(),
                                    "etn" to lastEditText?.getIdOrTag().orEmpty(),
                                    "et" to "text"
                                ),
                                ts = ts,
                                //sm = "",
                                //pd = "",
                                v = "S~C~~${actualText.length}"
                            )
                        )

                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = BLUR,
                                tg = hashMapOf(
                                    "tgs" to lastEditText?.getIdOrTag().orEmpty()
                                ),
                                ts = ts
                            )
                        )
                    null
                }
            }
        }
    }
}