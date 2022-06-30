package com.neuroid.tracker.callbacks

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.neuroid.tracker.utils.NIDLog


class NIDGyroscopeListener(val callback: (data: AxisData) -> Unit) :
    SensorEventListener {

    companion object {
        private const val TAG = "NIDGyroscopeListener"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // Axis of the rotation sample, not normalized yet.
            val axisX: Float = event.values[0]
            val axisY: Float = event.values[1]
            val axisZ: Float = event.values[2]

            //NIDLog.d(TAG, "axisX: $axisX axisY: $axisY axisZ: $axisZ")
            callback(AxisData(it.sensor?.type ?: 0, axisX, axisY, axisZ))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        NIDLog.d(TAG, "onAccuracyChanged")
    }
}

class AxisData(val type: Int, val axisX: Float, val axisY: Float, val axisZ: Float)