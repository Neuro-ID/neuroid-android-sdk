package com.neuroid.tracker.utils

import android.os.CountDownTimer
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

object NIDTimerActive {
    private val timer = object: CountDownTimer(30000, 1000) {
        var isSensorRunning = false

        override fun onTick(p0: Long) {
            isSensorRunning = true
            // No op
        }

        override fun onFinish() {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            NIDSensorHelper.stopSensors()
            isSensorRunning = false
            // USER_INACTIVE code would go here.
            // we cancel timer when there is any event. If the timer finishes, it means no event

        }

    }

    fun initTimer() {
        timer.start()
    }


    @Synchronized fun restartTimerActive() {
        timer.cancel()
        if (!timer.isSensorRunning) {
            // restart the sensors since the sensors were stopped at timeout
            NIDSensorHelper.restartSensors()
        }
        timer.start()
    }
}