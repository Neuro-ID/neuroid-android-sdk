package com.neuroid.tracker.utils

import android.util.Log
import com.neuroid.tracker.BuildConfig

object NIDLog {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.d(tag, it)
            }
            Log.d(tag, msg)
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
            Log.i(tag, msg)
        }
    }

    fun v(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.v(tag, it)
            }
            Log.v(tag, msg)
        }
    }

    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            msg.chunked(900).forEach {
                Log.w(tag, it)
            }
            Log.v(tag, msg)
        }
    }
}