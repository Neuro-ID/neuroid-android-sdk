package com.neuroid.tracker.events

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.view.ReactViewGroup
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.getIdOrTag

class NIDTouchEventManager(
    private val viewParent: ViewGroup
) {
    private var lastView: View? = null
    private var lastViewName = ""
    private var lastTypeOfView = 0

    fun detectView(motionEvent: MotionEvent?, timeMills: Long): View? {
        return motionEvent?.let {
            val currentView = getView(viewParent, motionEvent.x, motionEvent.y)
            val nameView = currentView?.getIdOrTag() ?: "main_view"

            detectChangesOnView(currentView, timeMills, motionEvent.action)

            val typeOfView = when(currentView) {
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
                                    tg = hashMapOf(
                                        "tgs" to ""
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    )
                                )
                            )

                        if (typeOfView == 2) {
                            getDataStoreInstance()
                                .saveEvent(
                                    NIDEventModel(
                                        type = FOCUS,
                                        ts = timeMills,
                                        tg = hashMapOf(
                                            "tgs" to lastViewName
                                        )
                                    )
                                )
                        }
                    }
                }
                ACTION_MOVE -> {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_MOVE,
                                ts = timeMills,
                                tg = hashMapOf(
                                    "tgs" to ""
                                ),
                                touches = listOf(
                                    "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                )
                            )
                        )
                }
                ACTION_UP -> {
                    if (lastTypeOfView > 0) {

                        if (lastTypeOfView == 2) {
                            getDataStoreInstance()
                                .saveEvent(
                                    NIDEventModel(
                                        type = BLUR,
                                        ts = timeMills,
                                        tg = hashMapOf(
                                            "tgs" to lastViewName
                                        )
                                    )
                                )
                        }

                        lastTypeOfView = 0
                        lastViewName = ""

                        getDataStoreInstance()
                            .saveEvent(
                                NIDEventModel(
                                    type = TOUCH_END,
                                    ts = timeMills,
                                    tg = hashMapOf(
                                        "tgs" to ""
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    )
                                )
                            )
                    }
                }
            }
            currentView
        }
    }

    private fun getView(subView: ViewGroup, x: Float, y: Float): View? {
        val view = subView.children.firstOrNull {
            val location = IntArray(2)
            it.getLocationInWindow(location)
            (x >= location[0] && x <= location[0] + it.width && y >= location[1] && y <= location[1] + it.height)
        }

        return when (view) {
            is Spinner -> view
            is ViewGroup -> getView(view, x, y)
            else -> view
        }
    }

    private fun detectChangesOnView(currentView: View?, timeMills: Long, action: Int) {
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
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = type,
                                tg = hashMapOf(
                                    "tgs" to nameView,
                                    "etn" to INPUT
                                ),
                                ts = timeMills
                            )
                        )
                }
            } else {
                if (lastView is SeekBar) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SLIDER_CHANGE,
                                tg = hashMapOf(
                                    "tgs" to nameView,
                                    "etn" to INPUT
                                ),
                                v = ((lastView as SeekBar).progress).toString(),
                                ts = System.currentTimeMillis()
                            )
                        )
                }
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }
}