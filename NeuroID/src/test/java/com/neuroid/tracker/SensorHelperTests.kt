package com.neuroid.tracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.neuroid.tracker.callbacks.NIDSensorData
import com.neuroid.tracker.callbacks.NIDSensorGenListener
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.callbacks.NIDSensorStatus
import com.neuroid.tracker.callbacks.NIDSensors
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

class SensorHelperTests {
    @Before
    fun setup() {
        NIDSensorHelper.resetSensorManager()
    }

    @Test
    fun initSensorHelper_gyro_test() {
        val nidSensors = mockk<NIDSensors>()
        val logger = mockk<NIDLogWrapper>()
        every { logger.i(any(  ), any()) } just runs
        val gyro = mockk<Sensor>()
        every { gyro.type } returns Sensor.TYPE_GYROSCOPE
        every { gyro.name } returns "gyro"
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(Sensor.TYPE_ALL) } returns listOf(gyro)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns gyro
        every { sensorManager.registerListener(any<NIDSensorGenListener>(), any(), any(), any<Int>())} returns true
        every { sensorManager.requestTriggerSensor(any(), any())} returns true
        val context = mockk<Context>()
        every { context.getSystemService<SensorManager>() } returns sensorManager
        NIDSensorHelper.initSensorHelper(context, logger, nidSensors)
        // is it the proper sensor?
        verify { logger.i("NIDSensorHelper", "Sensor:gyro 4") }
        // did we register the listener?
        verify { sensorManager.registerListener(any(), gyro, 10000, 10000) }
    }

    @Test
    fun initSensorHelper_accel_test() {
        val nidSensors = mockk<NIDSensors>()
        val logger = mockk<NIDLogWrapper>()
        every { logger.i(any(), any()) } just runs
        val accel = mockk<Sensor>()
        every { accel.type } returns Sensor.TYPE_ACCELEROMETER
        every { accel.name } returns "accel"
        val sensorManager = mockk<SensorManager>()
        every { sensorManager.getSensorList(Sensor.TYPE_ALL) } returns listOf(accel)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns accel
        every { sensorManager.registerListener(any<NIDSensorGenListener>(), any(), any(), any<Int>())} returns true
        val context = mockk<Context>()
        every { context.getSystemService<SensorManager>() } returns sensorManager
        NIDSensorHelper.initSensorHelper(context, logger, nidSensors)
        // is it the proper sensor?
        verify { logger.i("NIDSensorHelper", "Sensor:accel 1") }
        // did we register the listener?
        verify { sensorManager.registerListener(any(), accel, 10000, 10000) }
    }

    @Test
    fun test_sensorListener_gyroscope() {
        // prepare the gyroscope event (reflection)
        val sensorEvent = mockk<SensorEvent>()
        val sensor = mockk<Sensor>()
        every{sensor.type} returns Sensor.TYPE_GYROSCOPE
        val sensorField: Field = SensorEvent::class.java.getField("sensor")
        sensorField.isAccessible = true
        sensorField.set(sensorEvent, sensor)
        val valuesField: Field = SensorEvent::class.java.getField("values")
        valuesField.isAccessible = true
        valuesField.set(sensorEvent, floatArrayOf(1.0f, 2.0f, 3.0f))

        //prepare the sensor
        val nidSensors = mockk<NIDSensors>()
        every {nidSensors.gyroscopeData.copy(any(), any(), any(), any(), any())} returns mockk()
        every {nidSensors.gyroscopeData = any()} just runs

        // prepare output receivers
        val fvGyro = mockk<NIDSensorData>()
        every{ fvGyro.axisX } returns null
        every{ fvGyro.axisX = capture(slot()) } just runs
        every{ fvGyro.axisY = capture(slot()) } just runs
        every{ fvGyro.axisZ = capture(slot()) } just runs
        every{ fvGyro.status = capture(slot()) } just runs
        val fvAccel = mockk<NIDSensorData>()

        val listener = NIDSensorHelper.getNIDGenListener(nidSensors, fvGyro, fvAccel)

        // fire the sensor event!
        listener.onSensorChanged(sensorEvent)

        // did we call copy on the gyroscope data?
        verify { nidSensors.gyroscopeData.copy(any(), any(), 1.0F, 2.0F, 3.0F) }
        verify { fvGyro setProperty "axisX" value 1.0F }
        verify { fvGyro setProperty "axisY" value 2.0F }
        verify { fvGyro setProperty "axisZ" value 3.0F }
        verify { fvGyro setProperty "status" value any<NIDSensorStatus>() }
    }

    @Test
    fun test_sensorListener_accelerometer() {
        // prepare the accelerometer event (reflection)
        val sensorEvent = mockk<SensorEvent>()
        val sensor = mockk<Sensor>()
        every{sensor.type} returns Sensor.TYPE_ACCELEROMETER
        val sensorField: Field = SensorEvent::class.java.getField("sensor")
        sensorField.isAccessible = true
        sensorField.set(sensorEvent, sensor)
        val valuesField: Field = SensorEvent::class.java.getField("values")
        valuesField.isAccessible = true
        valuesField.set(sensorEvent, floatArrayOf(11.0f, 22.0f, 33.0f))

        //prepare the sensor
        val nidSensors = mockk<NIDSensors>()
        every {nidSensors.accelerometer.copy(any(),any(),any(), any(), any()) } returns mockk()
        every {nidSensors.accelerometer = any()} just runs

        // prepare output receivers
        val fvGyro = mockk<NIDSensorData>()
        val fvAccel = mockk<NIDSensorData>()
        every{ fvAccel.axisX } returns null
        every{ fvAccel.axisX = capture(slot()) } just runs
        every{ fvAccel.axisY = capture(slot()) } just runs
        every{ fvAccel.axisZ = capture(slot()) } just runs
        every{ fvAccel.status = capture(slot()) } just runs

        val listener = NIDSensorHelper.getNIDGenListener(nidSensors, fvGyro, fvAccel)

        // fire the sensor event!
        listener.onSensorChanged(sensorEvent)

        // did we call copy on the accelerometer data?
        verify { nidSensors.accelerometer.copy(any(), any(), 11.0F, 22.0F, 33.0F) }
        verify { fvAccel setProperty "axisX" value 11.0F }
        verify { fvAccel setProperty "axisY" value 22.0F }
        verify { fvAccel setProperty "axisZ" value 33.0F }
        verify { fvAccel setProperty "status" value any<NIDSensorStatus>() }
    }
}