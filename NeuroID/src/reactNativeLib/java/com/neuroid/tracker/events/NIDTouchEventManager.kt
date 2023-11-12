package com.neuroid.tracker.events

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.extensions.getIdOrTag
import org.json.JSONArray
import org.json.JSONObject

class NIDTouchEventManager(
    private val viewParent: ViewGroup
): TouchEventManager() {
    private var lastView: View? = null
    private var lastViewName = ""
    private var lastTypeOfView = 0

    override fun detectView(motionEvent: MotionEvent?, timeMills: Long): View? {
        return motionEvent?.let {
            val currentView = getView(viewParent, motionEvent.x, motionEvent.y)
            val nameView = currentView?.getIdOrTag() ?: "main_view"

            detectChangesOnView(currentView, timeMills, motionEvent.action)

            val typeOfView = when (currentView) {
                is EditText,
                is ReactEditText,
                is CheckBox,
                is RadioButton,
                is ToggleButton,
                is Switch,
                is ImageButton,
                is SeekBar,
                is Spinner
                -> 1
                is Button -> 2
                is ReactViewGroup -> {
                    if (currentView.isFocusable) {
                        2
                    } else {
                        0
                    }
                }
                else -> 0
            }

            var v = ""
            val metadataObj = JSONObject()

            when (currentView) {
                is EditText -> {
                    v = "S~C~~${currentView.text.length}"
                }
                is RadioButton -> {
                    metadataObj.put("type", "radioButton")
                    metadataObj.put("id", "${currentView.getIdOrTag()}")

                    // go up to 3 parents in case a RadioGroup is not the direct parent
                    var rParent = currentView.parent;
                    repeat(3) { index ->
                        if (rParent is RadioGroup) {
                            val p = rParent as RadioGroup
                            metadataObj.put("rGroupId", "${p.getIdOrTag()}")
                            return@repeat
                        } else {
                            rParent = rParent.parent
                        }
                    }
                }
            }

            var motionValues = JSONObject()
            try {
                motionValues = generateMotionEventValues(motionEvent)
            } catch (ex: Exception) {
                NIDLog.d(msg="TouchEventManager - no motion error: ${ex.printStackTrace()}")
            }
            val rawAction = JSONObject().put("rawAction", it.action)
            val attrJSON = JSONArray().put(rawAction).put(metadataObj).put(motionValues)

            when (it.action) {
                ACTION_DOWN -> {
                    if (typeOfView > 0) {
                        lastViewName = nameView
                        lastTypeOfView = typeOfView

                        getDataStoreInstance()
                            .saveEvent(
                                NIDEventModel(
                                    type = TOUCH_START,
                                    ts = timeMills,
                                    tgs = nameView,
                                    tg = hashMapOf(
                                        "etn" to nameView,
                                        "sender" to nameView,
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    v = v,
                                    attrs = attrJSON
                                )
                            )
                    }
                }
                ACTION_MOVE -> {

                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_MOVE,
                                ts = timeMills,
                                tgs = nameView,
                                tg = hashMapOf(
                                    "etn" to nameView,
                                    "sender" to nameView,
                                ),
                                touches = listOf(
                                    "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                ),
                                v = v,
                                attrs = attrJSON


                            )
                        )
                }
                ACTION_UP -> {
                    if (lastTypeOfView > 0) {

                        lastTypeOfView = 0
                        lastViewName = ""

                        getDataStoreInstance()
                            .saveEvent(
                                NIDEventModel(
                                    type = TOUCH_END,
                                    ts = timeMills,
                                    tgs = nameView,
                                    tg = hashMapOf(
                                        "etn" to nameView,
                                        "sender" to nameView,
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    v = v,
                                    attrs = attrJSON

                                )
                            )
                    }
                }
            }
            currentView
        }
    }

    override fun detectChangesOnView(currentView: View?, timeMills: Long, action: Int) {
        var type = ""
        val nameView = currentView?.getIdOrTag().orEmpty()

        if (action == ACTION_UP) {
            if (lastView == currentView) {
                when (currentView) {
                    is CheckBox -> {
                        type = CHECKBOX_CHANGE
                        Log.i(
                            NIDLog.CHECK_BOX_CHANGE_TAG,
                            NIDLog.CHECK_BOX_ID + currentView.getIdOrTag()
                        )
                    }
                    is RadioButton -> {
                        type = RADIO_CHANGE
                        Log.i(
                            NIDLog.RADIO_BUTTON_CHANGE_TAG,
                            NIDLog.RADIO_BUTTON_ID + currentView.getIdOrTag()
                        )
                    }
                    is SeekBar -> {
                        type = SLIDER_CHANGE
                    }
                    else -> {
                        // Null
                    }
                }

                if (type.isNotEmpty()) {
                    // do nothing
                }
            } else {
                if (lastView is SeekBar) {
                    // do nothing!
                }
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }
}