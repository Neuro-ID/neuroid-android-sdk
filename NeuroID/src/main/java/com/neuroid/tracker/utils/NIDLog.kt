package com.neuroid.tracker.utils

import android.util.Log
import com.neuroid.tracker.NeuroID

object NIDLog {
    val nidTag = "NeuroID"
    val infoTag = "NeuroID Info"
    val debugTag = "NeuroID Debug"
    val errorTag = "****** NeuroID ERROR: ******"
    val warnTag = "NeuroID Warn"

    fun d(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        if (NeuroID.showLogs && NeuroID.isSDKStarted) {
            cb().chunked(900).forEach {
                Log.d(appendTag(tag, debugTag), it)
            }
        }
    }

    fun i(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        if (NeuroID.showLogs) {
            cb().chunked(900).forEach {
                Log.d(appendTag(tag, infoTag), it)
            }
        }
    }

    fun v(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        if (NeuroID.showLogs) {
            cb().chunked(900).forEach {
                Log.d(appendTag(tag, nidTag), it)
            }
        }
    }

    fun w(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        if (NeuroID.showLogs) {
            cb().chunked(900).forEach {
                Log.d(appendTag(tag, warnTag), it)
            }
        }
    }

    fun e(
        tag: String? = null,
        msg: String,
        cb: () -> String = { msg },
    ) {
        if (NeuroID.showLogs) {
            cb().chunked(900).forEach {
                Log.d(appendTag(tag, errorTag), it)
            }
        }
    }

    // Add default tag to provided optional tag
    private fun appendTag(
        tag: String? = null,
        levelTag: String,
    ): String {
        return if (tag != null) {
            "$levelTag $tag"
        } else {
            levelTag
        }
    }

    const val CHECK_BOX_CHANGE_TAG = "CheckBoxChange"
    const val CHECK_BOX_ID = "CheckBoxID:"
    const val RADIO_BUTTON_CHANGE_TAG = "RadioButtonChange"
    const val RADIO_BUTTON_ID = "RadioButtonID:"
}
