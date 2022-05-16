package com.neuroid.tracker.events

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Spinner
import androidx.core.view.children
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.getIdOrTag

class NIDTouchEventManager(
    private val viewParent: ViewGroup
) {
    private var lastView: View? = null

    fun detectView(motionEvent: MotionEvent?, timeMills: Long) {
        motionEvent?.let {
            val currentView = getView(viewParent, motionEvent.x, motionEvent.y)
            val nameView = currentView?.getIdOrTag() ?: "main_view"

            detectChangesOnView(currentView,timeMills, motionEvent.action)

            when(it.action) {
                ACTION_DOWN -> {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_START,
                                ts = timeMills,
                                tg = hashMapOf(
                                    "etn" to nameView,
                                    "tgs" to nameView,
                                    "sender" to nameView
                                )
                            )
                        )
                }
                ACTION_MOVE -> {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_MOVE,
                                x = it.x,
                                y = it.y,
                                ts = timeMills
                            )
                        )
                }
                ACTION_UP -> {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_END,
                                x = it.x,
                                y = it.y,
                                ts = timeMills
                            )
                        )
                }
            }
        }
    }

    private fun getView(subView: ViewGroup, x: Float, y: Float): View? {
        val view = subView.children.firstOrNull {
            val location = IntArray(2)
            it.getLocationInWindow(location)
            (x >= location[0] && x <= location[0] + it.width &&  y >= location[1] && y <= location[1] + it.height)
        }

        return when(view) {
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
                when(currentView) {
                    is CheckBox -> {
                        type = CHECKBOX_CHANGE
                    }
                    is RadioButton -> {
                        type = RADIO_CHANGE
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
                            ))
                }
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }
}