package com.neuroid.tracker.utils

import android.util.Log
import com.neuroid.tracker.BuildConfig

object NIDLog {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.d(tag, it)
            }
        }
    }

    fun e(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.i(tag, it)
            }
        }
    }

    fun v(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.v(tag, it)
            }
        }
    }

    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.w(tag, it)
            }
        }
    }
}