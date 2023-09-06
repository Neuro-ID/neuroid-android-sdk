package com.neuroid.tracker.utils

class NIDLogWrapper {
    companion object {
        var nidLogWrapper: NIDLogWrapper = NIDLogWrapper()
    }

    fun d(tag: String, msg: String) {
        NIDLog.d(tag, msg)
    }

    fun e(tag: String, msg: String) {
        NIDLog.e(tag, msg)
    }

    fun i(tag: String, msg: String) {
        NIDLog.i(tag, msg)
    }

    fun v(tag: String, msg: String) {
        NIDLog.v(tag, msg)
    }

    fun w(tag: String, msg: String) {
        NIDLog.w(tag, msg)
    }
}