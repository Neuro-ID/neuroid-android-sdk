package com.neuroid.tracker.events

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children

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

    fun generateMotionEventValues(motionEvent: MotionEvent): Map<String, Any> {
        var pointers = generatePointerValues(motionEvent.pointerCount, motionEvent)

        var yValues = generateYValues(motionEvent)
        var xValues = generateXValues(motionEvent)

        var size = motionEvent.size

        val metadataObj = mutableMapOf<String, Any>()
        metadataObj.put("pointerCount", 0)
        if (motionEvent?.pointerCount != null) {
            metadataObj["pointerCount"] = motionEvent?.pointerCount as Int
        }
        metadataObj.put("pointers", pointers)

        metadataObj.put("yValues", yValues)
        metadataObj.put("xValues", xValues)

        metadataObj.put("pressure", 0)
        if (motionEvent?.pressure != null) {
            metadataObj["pressure"] = motionEvent?.pressure as Float
        }
        metadataObj.put("hSize", motionEvent.historySize)
        metadataObj.put("size", size)

        return metadataObj
    }

    private fun generatePointerValues(pointerCount: Int, motionEvent: MotionEvent): Map<String, Any> {
        val pointerObj = mutableMapOf<String, Any>()

        for (i in 0 until pointerCount) {
            var mProp = MotionEvent.PointerProperties()
            motionEvent.getPointerProperties(
                motionEvent.getPointerId(i),
                mProp,
            )

            val pointerDetailsObj =  mutableMapOf<String, Any>()
            pointerDetailsObj.put("mPropId", mProp.id)
            pointerDetailsObj.put("mPropToolType", mProp.toolType)

            var pHistorySize = motionEvent.getHistorySize()
            if (pHistorySize > 0) {
                val xHistoryArray = mutableListOf<Float>()
                val yHistoryArray = mutableListOf<Float>()

                for (hi in 0 until pHistorySize) {
                    var hY = motionEvent.getHistoricalY(i, hi)
                    var hX = motionEvent.getHistoricalX(i, hi)

                    xHistoryArray.add(hX)
                    yHistoryArray.add(hY)
                }

                pointerDetailsObj.put("historicalX", xHistoryArray)
                pointerDetailsObj.put("historicalY", yHistoryArray)
            }

            pointerObj.put("$i", pointerDetailsObj)
        }

        return pointerObj
    }

    private fun generateYValues(motionEvent: MotionEvent): Map<String, Any> {
        val metadataObj = mutableMapOf<String, Any>()
        metadataObj.put("y",
            if(motionEvent?.y != null) {
                motionEvent.y
            } else {
                0
            }
        )
        metadataObj.put("yP",
            if(motionEvent?.yPrecision != null) {
                motionEvent.yPrecision
            } else {
                0
            }
        )
        metadataObj.put("yR",
            if(motionEvent?.rawY != null) {
                motionEvent.rawY
            } else {
                0
            }
        )
        metadataObj.put("yCalc",
            if(motionEvent?.rawY != null && motionEvent?.yPrecision != null) {
                motionEvent.rawY * motionEvent.yPrecision
            } else {
                0
            }
        )

        return metadataObj
    }

    private fun generateXValues(motionEvent: MotionEvent): Map<String, Any> {
        val metadataObj = mutableMapOf<String, Any>()
        metadataObj.put("x",
            if(motionEvent?.x != null) {
                motionEvent.x
            } else {
                0
            }
        )
        metadataObj.put("xP",
            if(motionEvent?.xPrecision != null) {
                motionEvent.xPrecision
            } else {
                0
            }
        )
        metadataObj.put("xR",
            if(motionEvent?.rawX != null) {
                motionEvent.rawX
            } else {
                0
            }
        )
        metadataObj.put("xCalc",
            if(motionEvent?.rawX != null && motionEvent?.xPrecision != null) {
                motionEvent.rawX * motionEvent.xPrecision
            } else {
                0
            }
        )

        return metadataObj
    }
}