package com.neuroid.tracker.events

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import org.json.JSONArray
import org.json.JSONObject

abstract class TouchEventManager {
    abstract fun detectView(motionEvent: MotionEvent?, timeMills: Long): View?

    abstract fun detectChangesOnView(currentView: View?, timeMills: Long, action: Int)

    fun getView(subView: ViewGroup, x: Float, y: Float): View? {
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

    fun generateMotionEventValues(motionEvent: MotionEvent): JSONObject {
        var pointers = generatePointerValues(motionEvent.pointerCount, motionEvent)

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
        metadataObj.put("yCalc", motionEvent.rawY * motionEvent.yPrecision)

        return metadataObj
    }

    private fun generateXValues(motionEvent: MotionEvent): JSONObject {
        val metadataObj = JSONObject()
        metadataObj.put("x", motionEvent?.x)
        metadataObj.put("xP", motionEvent?.xPrecision)
        metadataObj.put("xR", motionEvent?.rawX)
        metadataObj.put("xCalc", motionEvent.rawX * motionEvent.xPrecision)

        return metadataObj
    }
}