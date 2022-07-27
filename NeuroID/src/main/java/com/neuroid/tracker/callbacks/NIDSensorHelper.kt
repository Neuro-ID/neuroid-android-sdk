package com.neuroid.tracker.callbacks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.utils.NIDLog

object NIDSensorHelper {
    private const val TAG = "NIDSensorHelper"
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private val firstValuesGyro = NIDSensorData("Gyroscope")
    private val firstValuesAccel = NIDSensorData("Gyroscope")
    private val nidSensors: NIDSensors = NIDSensors()
    private var listener: NIDSensorGenListener? = null

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

        restartSensors()
    }

    private fun initSensorManager(context: Context) {
        if (sensorManager == null)
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(listener)
    }

    fun restartSensors() {
        listener = NIDSensorGenListener {
            when (it.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    nidSensors.gyroscopeData =
                        nidSensors.gyroscopeData.copy(
                            axisX = it.axisX,
                            axisY = it.axisY,
                            axisZ = it.axisZ
                        )

                    if (firstValuesGyro.axisX == null) {
                        firstValuesGyro.axisX = it.axisX
                        firstValuesGyro.axisY = it.axisY
                        firstValuesGyro.axisZ = it.axisZ
                        firstValuesGyro.status = NIDSensorStatus.AVAILABLE
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    nidSensors.accelerometer =
                        nidSensors.accelerometer.copy(
                            axisX = it.axisX,
                            axisY = it.axisY,
                            axisZ = it.axisZ
                        )

                    if (firstValuesAccel.axisX == null) {
                        firstValuesAccel.axisX = it.axisX
                        firstValuesAccel.axisY = it.axisY
                        firstValuesAccel.axisZ = it.axisZ
                        firstValuesAccel.status = NIDSensorStatus.AVAILABLE
                    }
                }
            }
        }

        gyroscopeSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
        }
    }

    fun getAccelerometerInfo() = NIDSensorModel(
        nidSensors.accelerometer.axisX,
        nidSensors.accelerometer.axisY,
        nidSensors.accelerometer.axisZ
    )
    fun getGyroscopeInfo() = NIDSensorModel(
        nidSensors.gyroscopeData.axisX,
        nidSensors.gyroscopeData.axisY,
        nidSensors.gyroscopeData.axisZ
    )

    val valuesGyro: NIDSensorData get() = firstValuesGyro
    val valuesAccel: NIDSensorData get() = firstValuesAccel
    val isSensorGyroAvailable: Boolean get() = firstValuesGyro.status == NIDSensorStatus.AVAILABLE
    val isSensorAccelAvailable: Boolean get() = firstValuesAccel.status == NIDSensorStatus.AVAILABLE
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
    var axisX: Float? = null,
    var axisY: Float? = null,
    var axisZ: Float? = null
) {
    override fun equals(other: Any?): Boolean =
        (other is NIDSensorData) &&
                name.contentEquals(other.name) &&
                status == other.status &&
                axisX == other.axisX &&
                axisY == other.axisY &&
                axisZ == other.axisZ
}