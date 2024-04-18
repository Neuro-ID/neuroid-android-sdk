package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig
import com.neuroid.tracker.NeuroIDImpl

object NIDVersion {
    fun getSDKVersion(): String {
        val rnText =
            NeuroIDImpl.getInternalInstance()?.isRN?.let {
                if (it) {
                    "rn-"
                } else {
                    ""
                }
            }

        return "5.android-" + "$rnText" + BuildConfig.VERSION_NAME
    }

    fun getInternalCurrentVersion(): String {
        return getSDKVersion() + " " + BuildConfig.GIT_HASH
    }
}
