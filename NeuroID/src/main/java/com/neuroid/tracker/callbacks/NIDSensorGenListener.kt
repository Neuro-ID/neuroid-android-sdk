package com.neuroid.tracker.callbacks

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class NIDSensorGenListener(val callback: (data: AxisData) -> Unit) :
    SensorEventListener {

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
        // No op
    }
}

class AxisData(val type: Int, val axisX: Float, val axisY: Float, val axisZ: Float)