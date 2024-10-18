package com.neuroid.tracker.callbacks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

enum class SensorTypes {
    GYRO,
    ACCEL,
}

class SensorHelperTests {
    // mock setup functions
    fun setupMockLogger(): NIDLogWrapper {
        val logger = mockk<NIDLogWrapper>()
        every { logger.i(any(), any()) } just runs

        return logger
    }

    fun setupMockSensor(type: SensorTypes): Sensor {
        val sensor = mockk<Sensor>()
        every { sensor.type } returns if (type == SensorTypes.GYRO) Sensor.TYPE_GYROSCOPE else Sensor.TYPE_ACCELEROMETER
        every { sensor.name } returns if (type == SensorTypes.GYRO) "gyro" else "accel"

        return sensor
    }

    fun setupMockSensorManager(type: SensorTypes): Pair<Sensor, SensorManager> {
        val sensor = setupMockSensor(type)
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(Sensor.TYPE_ALL) } returns listOf(sensor)
        every {
            sensorManager.getDefaultSensor(
                if (type == SensorTypes.GYRO) Sensor.TYPE_GYROSCOPE else Sensor.TYPE_ACCELEROMETER,
            )
        } returns sensor
        every {
            sensorManager.registerListener(
                any<NIDSensorGenListener>(),
                any(),
                any(),
                any<Int>(),
            )
        } returns true
        every { sensorManager.requestTriggerSensor(any(), any()) } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } returns Unit

        return Pair(sensor, sensorManager)
    }

    fun setupMockContext(sensorManager: SensorManager): Context {
        val context = mockk<Context>()
        every { context.getSystemService<SensorManager>() } returns sensorManager

        return context
    }

    fun setupMockSensorEvent(
        type: SensorTypes,
        floatArray: FloatArray,
    ): SensorEvent {
        val sensor = setupMockSensor(type)
        val sensorEvent = mockk<SensorEvent>()

        val sensorField: Field = SensorEvent::class.java.getField("sensor")
        sensorField.isAccessible = true
        sensorField.set(sensorEvent, sensor)

        val valuesField: Field = SensorEvent::class.java.getField("values")
        valuesField.isAccessible = true
        valuesField.set(sensorEvent, floatArray)

        return sensorEvent
    }

    fun setupMockNIDSensors(type: SensorTypes): NIDSensors {
        // prepare the sensor
        val nidSensors = mockk<NIDSensors>()
        if (type == SensorTypes.GYRO) {
            every {
                nidSensors.gyroscopeData.copy(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns mockk()
            every { nidSensors.gyroscopeData = any() } just runs
        } else {
            every {
                nidSensors.accelerometer.copy(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns mockk()
            every { nidSensors.accelerometer = any() } just runs
        }

        return nidSensors
    }

    fun setupMockNIDSensorData(type: SensorTypes): Pair<NIDSensorData, NIDSensorData> {
        // prepare output receivers
        val fvGyro = mockk<NIDSensorData>()
        val fvAccel = mockk<NIDSensorData>()

        if (type == SensorTypes.GYRO) {
            every { fvGyro.axisX } returns null
            every { fvGyro.axisX = capture(slot()) } just runs
            every { fvGyro.axisY = capture(slot()) } just runs
            every { fvGyro.axisZ = capture(slot()) } just runs
            every { fvGyro.status = capture(slot()) } just runs
        } else {
            every { fvAccel.axisX } returns null
            every { fvAccel.axisX = capture(slot()) } just runs
            every { fvAccel.axisY = capture(slot()) } just runs
            every { fvAccel.axisZ = capture(slot()) } just runs
            every { fvAccel.status = capture(slot()) } just runs
        }

        return Pair(fvGyro, fvAccel)
    }

    // verification Functions
    fun verifySensorPair(sensorPair: Pair<Sensor, SensorManager>) {
        verify {
            sensorPair.second.registerListener(
                any(),
                sensorPair.first,
                10000,
                10000,
            )
        }
    }

    fun verifyNIDSensorValues(
        type: SensorTypes,
        expectedValueArray: FloatArray,
        nidSensors: NIDSensors,
    ) {
        val sensor =
            if (type == SensorTypes.GYRO) {
                nidSensors.gyroscopeData
            } else {
                nidSensors.accelerometer
            }

        verify {
            sensor.copy(
                any(),
                any(),
                expectedValueArray[0],
                expectedValueArray[1],
                expectedValueArray[2],
            )
        }
    }

    fun verifySensorValues(
        sensor: NIDSensorData,
        xAxis: Float,
        yAxis: Float,
        zAxis: Float,
    ) {
        verify { sensor setProperty "axisX" value xAxis }
        verify { sensor setProperty "axisY" value yAxis }
        verify { sensor setProperty "axisZ" value zAxis }
        verify { sensor setProperty "status" value any<NIDSensorStatus>() }
    }

    @Before
    fun setup() {
        NIDSensorHelper.resetSensorManager()
    }

    @Test
    fun initSensorHelper_gyro_test() {
        val logger = setupMockLogger()
        val sensorPair = setupMockSensorManager(SensorTypes.GYRO)
        val context = setupMockContext(sensorPair.second)

        val nidSensors = mockk<NIDSensors>()

        NIDSensorHelper.initSensorHelper(context, logger, nidSensors)

        // is it the proper sensor?
        verify { logger.i("NeuroID SensorHelper", "Sensor:gyro 4") }
        // did we register the listener?
        verifySensorPair(sensorPair)
    }

    @Test
    fun initSensorHelper_accel_test() {
        val logger = setupMockLogger()
        val sensorPair = setupMockSensorManager(SensorTypes.ACCEL)
        val context = setupMockContext(sensorPair.second)

        val nidSensors = mockk<NIDSensors>()

        NIDSensorHelper.initSensorHelper(context, logger, nidSensors)

        // is it the proper sensor?
        verify { logger.i("NeuroID SensorHelper", "Sensor:accel 1") }
        // did we register the listener?
        verifySensorPair(sensorPair)
    }

    @Test
    fun test_sensorListener_gyroscope() {
        val expectedValueArray = floatArrayOf(1.0f, 2.0f, 3.0f)

        // prepare the gyroscope event (reflection)
        val sensorEvent = setupMockSensorEvent(SensorTypes.GYRO, expectedValueArray)
        val nidSensors = setupMockNIDSensors(SensorTypes.GYRO)
        val sensorPair = setupMockNIDSensorData(SensorTypes.GYRO)

        val listener =
            NIDSensorHelper.getNIDGenListener(nidSensors, sensorPair.first, sensorPair.second)

        // fire the sensor event!
        listener.onSensorChanged(sensorEvent)

        // did we call copy on the gyroscope data?
        verifyNIDSensorValues(SensorTypes.GYRO, expectedValueArray, nidSensors)

        verifySensorValues(
            sensorPair.first,
            expectedValueArray[0],
            expectedValueArray[1],
            expectedValueArray[2],
        )
    }

    @Test
    fun test_sensorListener_accelerometer() {
        val expectedValueArray = floatArrayOf(11.0f, 22.0f, 33.0f)

        // prepare the accelerometer event (reflection)
        val sensorEvent = setupMockSensorEvent(SensorTypes.ACCEL, expectedValueArray)
        val nidSensors = setupMockNIDSensors(SensorTypes.ACCEL)
        val sensorPair = setupMockNIDSensorData(SensorTypes.ACCEL)

        val listener =
            NIDSensorHelper.getNIDGenListener(nidSensors, sensorPair.first, sensorPair.second)

        // fire the sensor event!
        listener.onSensorChanged(sensorEvent)

        // did we call copy on the accelerometer data?
        verifyNIDSensorValues(SensorTypes.ACCEL, expectedValueArray, nidSensors)

        verifySensorValues(
            sensorPair.second,
            expectedValueArray[0],
            expectedValueArray[1],
            expectedValueArray[2],
        )
    }
}
