package com.neuroid.tracker.utils

class NIDLogWrapper {
    fun d(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        NIDLog.d(tag, msg, cb)
    }

    fun e(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        NIDLog.e(tag, msg, cb)
    }

    fun i(
        tag: String? = null,
        msg: String,
    ) {
        NIDLog.i(tag, msg)
    }

    fun v(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        NIDLog.v(tag, msg, cb)
    }

    fun w(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        NIDLog.w(tag, msg, cb)
    }
}
