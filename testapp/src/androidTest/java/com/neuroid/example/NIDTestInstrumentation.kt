package com.neuroid.example

import okhttp3.mockwebserver.MockWebServer

object NIDTestInstrumentation {
    val server = MockWebServer()
    val recorder = EventRecorder()
    val attemptedRecorder = EventRecorder()
    private var started = false

    fun start() {
        if (!started) {
            val thread = Thread {
                server.start(8000)
                server.dispatcher = EventDispatcher(recorder)
            }
            thread.start()
            thread.join()
            started = true
        }
    }

    fun shutdown() {
        if (started) {
            try {
                server.shutdown()
            } catch (_: Exception) {
                // open keep-alive connections prevent clean drain; safe to ignore at process exit
            }
            started = false
        }
    }

    fun clearRecorders() {
        recorder.clear()
        attemptedRecorder.clear()
    }
}