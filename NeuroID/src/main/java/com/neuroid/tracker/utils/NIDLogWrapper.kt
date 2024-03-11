package com.neuroid.tracker.utils

class NIDLogWrapper {
    fun d(
        tag: String? = null,
        msg: String,
    ) {
        NIDLog.d(tag, msg)
    }

    fun e(
        tag: String? = null,
        msg: String,
    ) {
        NIDLog.e(tag, msg)
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
    ) {
        NIDLog.v(tag, msg)
    }

    fun w(
        tag: String? = null,
        msg: String,
    ) {
        NIDLog.w(tag, msg)
    }
}
