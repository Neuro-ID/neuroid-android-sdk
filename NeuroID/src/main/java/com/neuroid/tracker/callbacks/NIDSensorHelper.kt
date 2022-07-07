package com.neuroid.tracker.callbacks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.neuroid.tracker.utils.NIDLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object NIDSensorHelper {
    private const val TAG = "NIDSensorHelper"
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private val nidSensors: NIDSensors = NIDSensors()

    fun initSensorHelper(context: Context) {
        initSensorManager(context)

        val list = sensorManager?.getSensorList(Sensor.TYPE_ALL)
        list?.forEach { sensor ->
            when (sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeSensor =
                        sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                    nidSensors.gyroscopeData.status = NIDSensorStatus.AVAILABLE
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerSensor =
                        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                    nidSensors.accelerometer.status = NIDSensorStatus.AVAILABLE
                }
            }
            NIDLog.i(TAG, "Sensor:${sensor.name} ${sensor.type}")
        }
    }

    private fun initSensorManager(context: Context) {
        if (sensorManager == null)
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun getSensorInfo(): Flow<NIDSensors> = callbackFlow {
        val listener = NIDGyroscopeListener {
            when (it.type) {
                Sensor.TYPE_GYROSCOPE -> nidSensors.gyroscopeData =
                    nidSensors.gyroscopeData.copy(
                        axisX = it.axisX,
                        axisY = it.axisY,
                        axisZ = it.axisZ
                    )
                Sensor.TYPE_ACCELEROMETER -> nidSensors.accelerometer =
                    nidSensors.accelerometer.copy(
                        axisX = it.axisX,
                        axisY = it.axisY,
                        axisZ = it.axisZ
                    )
            }
            trySend(nidSensors)
        }
        gyroscopeSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
        }
        awaitClose {
            sensorManager?.unregisterListener(listener)
        }
    }

}

enum class NIDSensorStatus {
    AVAILABLE,
    UNAVAILABLE
}

data class NIDSensors(
    var gyroscopeData: NIDSensorData = NIDSensorData("Gyroscope"),
    var accelerometer: NIDSensorData = NIDSensorData("Accelerometer")
) {
    override fun equals(other: Any?): Boolean =
        (other is NIDSensors) &&
                gyroscopeData == other.gyroscopeData &&
                accelerometer == other.accelerometer
}

data class NIDSensorData(
    val name: String,
    var status: NIDSensorStatus = NIDSensorStatus.UNAVAILABLE,
    var axisX: Float = 0f,
    var axisY: Float = 0f,
    var axisZ: Float = 0f
) {
    override fun equals(other: Any?): Boolean =
        (other is NIDSensorData) &&
                name.contentEquals(other.name) &&
                status == other.status &&
                axisX == other.axisX &&
                axisY == other.axisY &&
                axisZ == other.axisZ
}