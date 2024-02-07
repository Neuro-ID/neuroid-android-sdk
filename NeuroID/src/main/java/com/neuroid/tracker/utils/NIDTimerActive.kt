package com.neuroid.tracker.utils

import android.os.CountDownTimer
import com.neuroid.tracker.callbacks.NIDSensorHelper

object NIDTimerActive {
    private val timer = object: CountDownTimer(30000, 1000) {
        var isSensorRunning = false

        override fun onTick(p0: Long) {
            isSensorRunning = true
        }

        override fun onFinish() {
            NIDSensorHelper.stopSensors()
            isSensorRunning = false
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