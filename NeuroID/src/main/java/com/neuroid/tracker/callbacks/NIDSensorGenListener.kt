package com.neuroid.tracker.callbacks

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLogWrapper

class NIDSensorGenListener(val logger: NIDLogWrapper = NIDLogWrapper(),
                           val callback: (data: AxisData) -> Unit) :
    SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            var axisX = -1F
            var axisY = -1F
            var axisZ = -1F
            // Axis of the rotation sample, not normalized yet.
            // Some devices may not provide all three values. Also possible that
            // the event values array is not initialized before we try to get data.
            try {
                if (event.values.isNotEmpty()) {
                    if (event.values.isNotEmpty()) {
                        axisX = event.values[0]
                    }
                    if (event.values.size > 1) {
                        axisY = event.values[1]
                    }
                    if (event.values.size > 2) {
                        axisZ = event.values[2]
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    "NIDSensorGenListener",
                    "Error getting sensor values: ${e.message}",
                )
            }

            callback(AxisData(it.sensor?.type ?: 0, axisX, axisY, axisZ))
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
        // No op
    }
}

class AxisData(val type: Int, val axisX: Float, val axisY: Float, val axisZ: Float)
