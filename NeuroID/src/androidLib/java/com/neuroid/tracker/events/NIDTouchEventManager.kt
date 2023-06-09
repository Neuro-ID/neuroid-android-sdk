package com.neuroid.tracker.events

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.ToggleButton
import android.widget.Switch
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.RatingBar
import android.widget.Button
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import com.neuroid.tracker.callbacks.NIDSensorHelper
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
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            var motionValues = generateMotionEventValues(motionEvent)


            detectChangesOnView(currentView, timeMills, motionEvent.action)

            val typeOfView = when (currentView) {
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
//                is AutoCompleteTextView
                -> 1
                is Button -> 2
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
                                    tgs = nameView,
                                    tg = hashMapOf(
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "rawAction" to it.action
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}, " +
                                                motionValues +
                                                "}"
                                    ),
                                    gyro = gyroData,
                                    accel = accelData
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
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "rawAction" to it.action
                                ),
                                touches = listOf(
                                    "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}," +
                                            motionValues +
                                            "}"
                                ),
                                gyro = gyroData,
                                accel = accelData
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
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "rawAction" to it.action
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}," +
                                                motionValues +
                                                "}"
                                    ),
                                    gyro = gyroData,
                                    accel = accelData
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
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (action == ACTION_UP) {
            if (lastView == currentView) {
                when (currentView) {
                    is CheckBox, is AppCompatCheckBox -> {
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

                /* if (type.isNotEmpty()) {
                     getDataStoreInstance()
                         .saveEvent(
                             NIDEventModel(
                                 type = type,
                                 tg = hashMapOf(
                                     "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                     "tgs" to nameView,
                                     "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                 ),
                                 tgs = nameView,
                                 ts = timeMills,
                                 gyro = gyroData,
                                 accel = accelData
                             )
                         )
                 }*/
            } else {
                /*if (lastView is SeekBar) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SLIDER_CHANGE,
                                tg = hashMapOf(
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                ),
                                tgs = nameView,
                                v = ((lastView as SeekBar).progress).toString(),
                                ts = System.currentTimeMillis(),
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }*/
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }

    private fun generateMotionEventValues(motionEvent: MotionEvent): String {
        var pointers = generatePointerValues(motionEvent?.pointerCount, motionEvent)

        var yValues = generateYValues(motionEvent)
        var xValues = generateXValues(motionEvent)

        var size = motionEvent.size

        return "\"pointerCount\":${motionEvent?.pointerCount}," +
                "${pointers}," +

                "${yValues}," +
                "${xValues}," +

                "\"pressure\":${motionEvent?.pressure}," +
                "\"hSize\":${motionEvent.historySize}," +
                "\"size\":${size}"
    }

    private fun generatePointerValues(pointerCount: Int, motionEvent: MotionEvent): String {
        var pointString = "\"pointers\":{"
        for (i in 0 until pointerCount) {
            var mProp = MotionEvent.PointerProperties()
            motionEvent.getPointerProperties(
                motionEvent.getPointerId(i),
                mProp,
            )

            var pHistorySize = motionEvent.getHistorySize()

            pointString += "\"$i\":{ " +
                    "\"mPropId\":${
                        mProp.id
                    }," +
                    "\"mPropToolType\":${
                        mProp.toolType
                    }"

            if (pHistorySize > 0) {
                var yHistoryString = "["
                var xHistoryString = "["
                for (hi in 0 until pHistorySize) {
                    var hY = motionEvent.getHistoricalY(i, hi)
                    var hX = motionEvent.getHistoricalX(i, hi)

                    yHistoryString += "$hY,"
                    xHistoryString += "$hX,"

                    if (i + 1 == pHistorySize) {
                        yHistoryString = yHistoryString.dropLast(1)
                        xHistoryString = xHistoryString.dropLast(1)
                        break
                    }
                }
                yHistoryString += "]"
                xHistoryString += "]"

                pointString += ",\"historicalY\":${
                    yHistoryString
                },"
                pointString += "\"historicalX\":${
                    xHistoryString
                }"


            }

            pointString += "},"

            if (i + 1 == pointerCount) {
                pointString = pointString.dropLast(1)
                break
            }
        }

        pointString += "}"
        return pointString
    }

    private fun generateYValues(motionEvent: MotionEvent): String {
        return "\"yValues\":{" +
                "\"y\":${motionEvent?.y}," +
                "\"yP\":${motionEvent?.yPrecision}," +
                "\"yR\":${motionEvent?.rawY}," +
                "\"yCalc\":${motionEvent?.rawY * motionEvent?.yPrecision}" +
                "}"
    }

    private fun generateXValues(motionEvent: MotionEvent): String {
        return "\"xValues\":{" +
                "\"x\":${motionEvent?.x}," +
                "\"xP\":${motionEvent?.xPrecision}," +
                "\"xR\":${motionEvent?.rawX}," +
                "\"xCalc\":${motionEvent?.rawX * motionEvent?.xPrecision}" +
                "}"
    }
}