package com.neuroid.example

import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner

import org.junit.runners.model.FrameworkMethod

class RepeatedTestRunner(t: Class<*>?) : BlockJUnit4ClassRunner(t) {
    private val repeatCount = 1

    companion object {
        var currentId = 1
        var currentRegUserId = 1

        fun incrementId() {
            currentId += 1
        }

        fun incrementReguserId() {
            currentRegUserId += 1
        }
    }

    override fun runChild(method: FrameworkMethod?, notifier: RunNotifier?) {
        for (i in 0 until repeatCount) {
            super.runChild(method, notifier)
        }
    }
}