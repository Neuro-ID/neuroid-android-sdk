package com.neuroid.tracker.utils

import android.os.CountDownTimer
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

object NIDTimerActive {
    private val timer = object: CountDownTimer(30000, 1000) {
        override fun onTick(p0: Long) {
            // No op
        }

        override fun onFinish() {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = USER_INACTIVE,
                    ts = System.currentTimeMillis()
                ))
        }

    }

    fun initTimer() {
        timer.start()
    }


    @Synchronized fun restartTimerActive() {
        timer.cancel()
        timer.start()
    }
}