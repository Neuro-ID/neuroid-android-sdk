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
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.getIdOrTag
import org.json.JSONArray
import org.json.JSONObject

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
                is RadioGroup
//                is AutoCompleteTextView
                -> 1
                is Button -> 2
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


            val motionValues = generateMotionEventValues(motionEvent)
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
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    gyro = gyroData,
                                    accel = accelData,
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
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                ),
                                touches = listOf(
                                    "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                ),
                                gyro = gyroData,
                                accel = accelData,
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
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty(),
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    gyro = gyroData,
                                    accel = accelData,
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
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }

    private fun generateMotionEventValues(motionEvent: MotionEvent): JSONObject {
        var pointers = generatePointerValues(motionEvent?.pointerCount, motionEvent)

        var yValues = generateYValues(motionEvent)
        var xValues = generateXValues(motionEvent)

        var size = motionEvent.size

        val metadataObj = JSONObject()
        metadataObj.put("pointerCount", motionEvent?.pointerCount)
        metadataObj.put("pointers", pointers)

        metadataObj.put("yValues", yValues)
        metadataObj.put("xValues", xValues)

        metadataObj.put("pressure", motionEvent?.pressure)
        metadataObj.put("hSize", motionEvent.historySize)
        metadataObj.put("size", size)

        return metadataObj
    }

    private fun generatePointerValues(pointerCount: Int, motionEvent: MotionEvent): JSONObject {
        val pointerObj = JSONObject()

        for (i in 0 until pointerCount) {
            var mProp = MotionEvent.PointerProperties()
            motionEvent.getPointerProperties(
                motionEvent.getPointerId(i),
                mProp,
            )

            val pointerDetailsObj = JSONObject()
            pointerDetailsObj.put("mPropId", mProp.id)
            pointerDetailsObj.put("mPropToolType", mProp.toolType)

            var pHistorySize = motionEvent.getHistorySize()
            if (pHistorySize > 0) {
                val xHistoryArray = JSONArray()
                val yHistoryArray = JSONArray()

                for (hi in 0 until pHistorySize) {
                    var hY = motionEvent.getHistoricalY(i, hi)
                    var hX = motionEvent.getHistoricalX(i, hi)

                    xHistoryArray.put(hX)
                    yHistoryArray.put(hY)
                }

                pointerDetailsObj.put("historicalX", xHistoryArray)
                pointerDetailsObj.put("historicalY", yHistoryArray)
            }

            pointerObj.put("$i", pointerDetailsObj)
        }

        return pointerObj
    }

    private fun generateYValues(motionEvent: MotionEvent): JSONObject {
        val metadataObj = JSONObject()
        metadataObj.put("y", motionEvent?.y)
        metadataObj.put("yP", motionEvent?.yPrecision)
        metadataObj.put("yR", motionEvent?.rawY)
        metadataObj.put("yCalc", motionEvent?.rawY * motionEvent?.yPrecision)

        return metadataObj
    }

    private fun generateXValues(motionEvent: MotionEvent): JSONObject {
        val metadataObj = JSONObject()
        metadataObj.put("x", motionEvent?.x)
        metadataObj.put("xP", motionEvent?.xPrecision)
        metadataObj.put("xR", motionEvent?.rawX)
        metadataObj.put("xCalc", motionEvent?.rawX * motionEvent?.xPrecision)

        return metadataObj
    }
}