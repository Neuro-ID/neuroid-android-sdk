package com.neuroid.tracker.events

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.core.view.children
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.getIdOrTag

class NIDTouchEventManager(
    private val viewParent: ViewGroup
) {
    fun detectView(motionEvent: MotionEvent?, timeMills: Long) {
        motionEvent?.let {
            when(it.action) {
                ACTION_DOWN -> {
                    getDataStoreInstance(viewParent.context)
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_START,
                                x = it.x,
                                y = it.y,
                                ts = timeMills
                            ).getOwnJson()
                        )
                }
                ACTION_MOVE -> {
                    getDataStoreInstance(viewParent.context)
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_MOVE,
                                x = it.x,
                                y = it.y,
                                ts = timeMills
                            ).getOwnJson()
                        )
                }
                ACTION_UP -> {
                    getDataStoreInstance(viewParent.context)
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_END,
                                x = it.x,
                                y = it.y,
                                ts = timeMills
                            ).getOwnJson()
                        )
                }
            }

            if (it.action == ACTION_UP) {
                val childView = getView(viewParent, it.x, it.y)

                when(childView) {
                    is CheckBox -> {
                        getDataStoreInstance(viewParent.context)
                            .saveEvent(
                                NIDEventModel(
                                    type = CHECKBOX_CHANGE,
                                    x = it.x,
                                    y = it.y,
                                    tgs = hashMapOf(
                                        "tgs" to childView.getIdOrTag()
                                    ),
                                    ts = timeMills
                                ).getOwnJson()
                            )
                    }
                    is RadioButton -> {
                        getDataStoreInstance(viewParent.context)
                            .saveEvent(
                                NIDEventModel(
                                    type = RADIO_CHANGE,
                                    x = it.x,
                                    y = it.y,
                                    tgs = hashMapOf(
                                        "tgs" to childView.getIdOrTag()
                                    ),
                                    ts = timeMills
                                ).getOwnJson()
                            )
                    }
                    else -> {
                        // Null
                    }
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

        return if (view is ViewGroup) {
            getView(view, x, y)
        } else {
            view
        }
    }
}