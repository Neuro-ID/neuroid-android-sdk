package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig

object NIDVersion {
    fun getSDKVersion(): String {
        return "4.android-" + BuildConfig.VERSION_NAME
    }

    fun getInternalCurrentVersion(): String {
        return getSDKVersion() + " " + BuildConfig.GIT_HASH
    }
}