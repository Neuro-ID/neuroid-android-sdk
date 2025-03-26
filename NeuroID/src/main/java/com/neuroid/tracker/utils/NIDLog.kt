package com.neuroid.tracker.utils

import android.util.Log
import com.neuroid.tracker.NeuroID

class NIDLog {
    val nidTag = "NeuroID"
    val infoTag = "NeuroID Info"
    val debugTag = "NeuroID Debug"
    val errorTag = "****** NeuroID ERROR: ******"
    val warnTag = "NeuroID Warn"

    fun d(
        tag: String? = null,
        msg: String
    ) {
        if (NeuroID.showLogs && NeuroID.isSDKStarted) {
            Log.d(appendTag(tag, debugTag), msg)
        }
    }

    fun i(
        tag: String? = null,
        msg: String
    ) {
        if (NeuroID.showLogs) {
            Log.d(appendTag(tag, infoTag), msg)
        }
    }

    fun v(
        tag: String? = null,
        msg: String
    ) {
        if (NeuroID.showLogs) {
            Log.d(appendTag(tag, nidTag), msg)
        }
    }

    fun w(
        tag: String? = null,
        msg: String,
    ) {
        if (NeuroID.showLogs) {
            Log.d(appendTag(tag, warnTag), msg)
        }
    }

    fun e(
        tag: String? = null,
        msg: String,
    ) {
        if (NeuroID.showLogs) {
            Log.d(appendTag(tag, errorTag), msg)
        }
    }

    fun printLine(
        tag: String?,
        logLevel: String,
        msg: String,
    ) {
        if (NeuroID.showLogs) {
            println("${appendTag(tag, logLevel)}, $msg" )
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

    companion object {
        const val CHECK_BOX_CHANGE_TAG = "CheckBoxChange"
        const val CHECK_BOX_ID = "CheckBoxID:"
        const val RADIO_BUTTON_CHANGE_TAG = "RadioButtonChange"
        const val RADIO_BUTTON_ID = "RadioButtonID:"
    }
}
