package com.neuroid.tracker.events

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.detectViewType
import com.neuroid.tracker.utils.getEtnSenderName

class TouchEventManager(
    private val viewParent: ViewGroup,
    internal val neuroID: NeuroID,
    internal val logger: NIDLogWrapper,
) {
    private var lastView: View? = null
    private var lastViewName = ""
    private var lastTypeOfView = 0

    fun detectView(
        motionEvent: MotionEvent?,
        timeMills: Long,
    ): View? {
        return motionEvent?.let {
            val currentView = getView(viewParent, motionEvent.x, motionEvent.y)
            val nameView = currentView?.getIdOrTag() ?: "main_view"
            val etnSenderName = getEtnSenderName(currentView)

            detectChangesOnView(currentView, timeMills, motionEvent.action)

            val typeOfView = detectViewType(currentView)

            var v = ""
            val metadataObj = mutableMapOf<String, Any>()

            when (currentView) {
                is EditText -> {
                    v = "S~C~~${currentView.text.length}"
                }
                is RadioButton -> {
                    metadataObj["type"] = "radioButton"
                    metadataObj["id"] = currentView.getIdOrTag()

                    // go up to 3 parents in case a RadioGroup is not the direct parent
                    var rParent = currentView.parent
                    repeat(3) { _ ->
                        if (rParent is RadioGroup) {
                            val p = rParent as RadioGroup
                            metadataObj["rGroupId"] = p.getIdOrTag()
                            return@repeat
                        } else {
                            rParent = rParent.parent
                        }
                    }
                }
            }

            var motionValues = mapOf<String, Any>()
            try {
                motionValues = generateMotionEventValues(motionEvent)
            } catch (ex: Exception) {
                logger.d("TouchEventManager", "no motion error: ${ex.printStackTrace()}")
            }

            val rawAction = mapOf("rawAction" to it.action)
            val attrJSON = listOf(rawAction, metadataObj, motionValues)

            var shouldSaveEvent = false
            var eventType = ""
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (typeOfView > 0) {
                        lastViewName = nameView
                        lastTypeOfView = typeOfView

                        shouldSaveEvent = true
                        eventType = TOUCH_START
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    shouldSaveEvent = true
                    eventType = TOUCH_MOVE
                }
                MotionEvent.ACTION_UP -> {
                    if (lastTypeOfView > 0) {
                        lastTypeOfView = 0
                        lastViewName = ""

                        shouldSaveEvent = true
                        eventType = TOUCH_END
                    }
                }
            }

            if (shouldSaveEvent && eventType.isNotEmpty()) {
                neuroID.captureEvent(
                    type = eventType,
                    tgs = nameView,
                    tg =
                        hashMapOf(
                            "etn" to etnSenderName,
                            "tgs" to nameView,
                            "sender" to etnSenderName,
                        ),
                    touches = listOf(NIDTouchModel(0f, it.x, it.y)),
                    v = v,
                    attrs = attrJSON,
                )
            }

            currentView
        }
    }

    fun detectChangesOnView(
        currentView: View?,
        timeMills: Long,
        action: Int,
    ) {
        var type = ""
        val nameView = currentView?.getIdOrTag().orEmpty()

        if (action == MotionEvent.ACTION_UP) {
            if (lastView == currentView) {
                when (currentView) {
                    is CheckBox, is AppCompatCheckBox -> {
                        type = CHECKBOX_CHANGE
                        logger.d(
                            NIDLog.CHECK_BOX_CHANGE_TAG,
                            NIDLog.CHECK_BOX_ID + currentView.getIdOrTag(),
                        )
                    }
                    is RadioButton -> {
                        type = RADIO_CHANGE
                        logger.d(
                            NIDLog.RADIO_BUTTON_CHANGE_TAG,
                            NIDLog.RADIO_BUTTON_ID + currentView.getIdOrTag(),
                        )
                    }
                    is Switch, is SwitchCompat -> {
                        type = SWITCH_CHANGE
                    }
                    is ToggleButton -> {
                        type = TOGGLE_BUTTON_CHANGE
                    }
                    is RatingBar -> {
                        type = RATING_BAR_CHANGE
                    }
                    is SeekBar -> {
                        type = SLIDER_CHANGE
                    }
                    else -> {
                        // Null
                    }
                }
            }
            lastView = null
        } else if (action == MotionEvent.ACTION_DOWN) {
            lastView = currentView
        }
    }

    fun getView(
        subView: ViewGroup,
        x: Float,
        y: Float,
    ): View? {
        val view =
            subView.children.firstOrNull {
                val location = IntArray(2)
                it.getLocationInWindow(location)
                (
                    x >= location[0] &&
                        x <= location[0] + it.width &&
                        y >= location[1] &&
                        y <= location[1] + it.height
                )
            }

        return when (view) {
            is Spinner -> view
            is ViewGroup -> getView(view, x, y)
            else -> view
        }
    }

    fun generateMotionEventValues(motionEvent: MotionEvent?): Map<String, Any> {
        val pointers =
            if (motionEvent != null) {
                generatePointerValues(motionEvent.pointerCount, motionEvent)
            } else {
                mutableMapOf<String, Any>()
            }

        val yValues =
            generateXYValues(
                "y",
                MotionEventValues(
                    motionEvent?.y,
                    motionEvent?.rawY,
                    motionEvent?.yPrecision,
                ),
            )

        val xValues =
            generateXYValues(
                "x",
                MotionEventValues(
                    motionEvent?.x,
                    motionEvent?.rawX,
                    motionEvent?.xPrecision,
                ),
            )

        val size = motionEvent?.size ?: 0

        val metadataObj = mutableMapOf<String, Any>()
        metadataObj["pointerCount"] = 0
        if (motionEvent?.pointerCount != null) {
            metadataObj["pointerCount"] = motionEvent?.pointerCount as Int
        }
        metadataObj["pointers"] = pointers

        metadataObj["yValues"] = yValues
        metadataObj["xValues"] = xValues

        metadataObj["pressure"] = 0
        if (motionEvent?.pressure != null) {
            metadataObj["pressure"] = motionEvent?.pressure as Float
        }
        metadataObj["hSize"] = motionEvent?.historySize ?: 0
        metadataObj["size"] = size

        return metadataObj
    }

    private fun generatePointerValues(
        pointerCount: Int,
        motionEvent: MotionEvent,
    ): Map<String, Any> {
        val pointerObj = mutableMapOf<String, Any>()

        for (i in 0 until pointerCount) {
            val mProp = MotionEvent.PointerProperties()
            motionEvent.getPointerProperties(
                motionEvent.getPointerId(i),
                mProp,
            )

            val pointerDetailsObj = mutableMapOf<String, Any>()
            pointerDetailsObj["mPropId"] = mProp.id
            pointerDetailsObj["mPropToolType"] = mProp.toolType

            val pHistorySize = motionEvent.getHistorySize()
            if (pHistorySize > 0) {
                val xHistoryArray = mutableListOf<Float>()
                val yHistoryArray = mutableListOf<Float>()

                for (hi in 0 until pHistorySize) {
                    val hY = motionEvent.getHistoricalY(i, hi)
                    val hX = motionEvent.getHistoricalX(i, hi)

                    xHistoryArray.add(hX)
                    yHistoryArray.add(hY)
                }

                pointerDetailsObj["historicalX"] = xHistoryArray
                pointerDetailsObj["historicalY"] = yHistoryArray
            }

            pointerObj["$i"] = pointerDetailsObj
        }

        return pointerObj
    }

    private fun generateXYValues(
        key: String,
        motionEvent: MotionEventValues,
    ): Map<String, Any> {
        val metadataObj = mutableMapOf<String, Any>()
        metadataObj[key] = motionEvent.a ?: 0
        metadataObj["${key}P"] = motionEvent.precision ?: 0
        metadataObj["${key}R"] = motionEvent.raw ?: 0

        metadataObj["${key}Calc"] =
            if (motionEvent.raw != null && motionEvent.precision != null) {
                motionEvent.raw * motionEvent.precision
            } else {
                0
            }

        return metadataObj
    }
}

internal fun detectBasicAndroidViewType(currentView: View?): Int {
    val typeOfView =
        when (currentView) {
            is EditText,
            is CheckBox,
            is RadioButton,
            is ToggleButton,
            is Switch,
            is SwitchCompat,
            is ImageButton,
            is SeekBar,
            is Spinner,
            is RatingBar,
            is RadioGroup,
            //      is AutoCompleteTextView
            -> 1
            is Button -> 2
            else -> 0
        }

    return typeOfView
}

internal data class MotionEventValues(val a: Float?, val precision: Float?, val raw: Float?)
