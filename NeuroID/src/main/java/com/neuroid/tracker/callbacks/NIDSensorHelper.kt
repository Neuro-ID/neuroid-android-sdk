package com.neuroid.tracker.callbacks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.utils.NIDLogWrapper

object NIDSensorHelper {
    private const val TAG = "NeuroID SensorHelper"
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private val firstValuesGyro = NIDSensorData("Gyroscope")
    private val firstValuesAccel = NIDSensorData("Accelerometer")
    private val nidSensors: NIDSensors = NIDSensors()
    private var listener: NIDSensorGenListener? = null

    private var sensorActive = false

    fun isSensorActive(): Boolean {
        return sensorActive
    }

    fun initSensorHelper(
        context: Context,
        logger: NIDLogWrapper,
        nSensors: NIDSensors = nidSensors,
    ) {
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
            logger.i(TAG, "Sensor:${sensor.name} ${sensor.type}")
        }

        restartSensors(nSensors)
    }

    private fun initSensorManager(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
    }

    @VisibleForTesting
    // only called by unit tests, do not call outside of tests
    internal fun resetSensorManager() {
        sensorManager = null
        sensorActive = false
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(listener)
        sensorActive = false
    }

    fun restartSensors(nSensors: NIDSensors = nidSensors) {
        if (sensorActive) {
            return
        }
        // need to unregister the listeners before restarting the sensors.
        stopSensors()

        listener = getNIDGenListener(nSensors, firstValuesGyro, firstValuesAccel)

        gyroscopeSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
            sensorActive = true
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(listener, it, 10_000, 10_000)
            sensorActive = true
        }
    }

    @VisibleForTesting
    internal fun getNIDGenListener(
        nSensors: NIDSensors,
        fvGyro: NIDSensorData,
        fvAccel: NIDSensorData,
    ): NIDSensorGenListener {
        return NIDSensorGenListener {
            when (it.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    nSensors.gyroscopeData =
                        nSensors.gyroscopeData.copy(
                            axisX = it.axisX,
                            axisY = it.axisY,
                            axisZ = it.axisZ,
                        )

                    fvGyro.axisX = it.axisX
                    fvGyro.axisY = it.axisY
                    fvGyro.axisZ = it.axisZ
                    fvGyro.status = NIDSensorStatus.AVAILABLE
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    nSensors.accelerometer =
                        nSensors.accelerometer.copy(
                            axisX = it.axisX,
                            axisY = it.axisY,
                            axisZ = it.axisZ,
                        )

                    fvAccel.axisX = it.axisX
                    fvAccel.axisY = it.axisY
                    fvAccel.axisZ = it.axisZ
                    fvAccel.status = NIDSensorStatus.AVAILABLE
                }
            }
        }
    }

    fun getAccelerometerInfo() =
        NIDSensorModel(
            nidSensors.accelerometer.axisX,
            nidSensors.accelerometer.axisY,
            nidSensors.accelerometer.axisZ,
        )

    fun getGyroscopeInfo() =
        NIDSensorModel(
            nidSensors.gyroscopeData.axisX,
            nidSensors.gyroscopeData.axisY,
            nidSensors.gyroscopeData.axisZ,
        )

    val valuesGyro: NIDSensorData get() = firstValuesGyro
    val valuesAccel: NIDSensorData get() = firstValuesAccel
}

enum class NIDSensorStatus {
    AVAILABLE,
    UNAVAILABLE,
}

data class NIDSensors(
    var gyroscopeData: NIDSensorData = NIDSensorData("Gyroscope"),
    var accelerometer: NIDSensorData = NIDSensorData("Accelerometer"),
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
    var axisZ: Float? = null,
) {
    override fun equals(other: Any?): Boolean =
        (other is NIDSensorData) &&
            name.contentEquals(other.name) &&
            status == other.status &&
            axisX == other.axisX &&
            axisY == other.axisY &&
            axisZ == other.axisZ
}
