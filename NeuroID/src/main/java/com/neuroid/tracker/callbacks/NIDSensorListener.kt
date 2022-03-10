package com.neuroid.tracker.callbacks

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.neuroid.tracker.utils.NIDLog

class NIDSensorListener: SensorEventListener {
    private var prevX = 0F
    private var prevY = 0F
    private var prevZ = 0F

    override fun onSensorChanged(event: SensorEvent?) {
        val x = event?.values?.get(0)
        val y = event?.values?.get(1)
        val z = event?.values?.get(2)

        if (x != prevX) {
            NIDLog.d("NeuroId", "Acceleration force in m/s2, x axis: $x" )
        }

        if (y != prevY) {
            NIDLog.d("NeuroId", "Acceleration force in m/s2, y axis: $y" )
        }

        if (z != prevZ) {
            NIDLog.d("NeuroId", "Acceleration force in m/s2, z axis: $z" )
        }

        prevX = x ?: 0F
        prevY = y ?: 0F
        prevZ = z ?: 0F
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}