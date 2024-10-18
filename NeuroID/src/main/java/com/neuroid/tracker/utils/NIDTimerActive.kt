package com.neuroid.tracker.utils

import android.os.CountDownTimer
import com.neuroid.tracker.callbacks.NIDSensorHelper

object NIDTimerActive {
    private val timer =
        object : CountDownTimer(30000, 1000) {
            override fun onTick(p0: Long) {
                // No op
            }

            override fun onFinish() {
                NIDSensorHelper.stopSensors()
            }
        }

    fun initTimer() {
        timer.start()
    }

    @Synchronized fun restartTimerActive() {
        timer.cancel()
        NIDSensorHelper.restartSensors()
        timer.start()
    }
}
