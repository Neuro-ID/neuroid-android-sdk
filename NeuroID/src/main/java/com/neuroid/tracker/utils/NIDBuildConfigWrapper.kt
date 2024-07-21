package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig

/**
 *  required for testing, cannot mockkStatic BuildConfig.
 */
class NIDBuildConfigWrapper {
    fun getBuildVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getGitHash(): String {
        return BuildConfig.GIT_HASH
    }

    fun getFlavor(): String {
        return BuildConfig.FLAVOR
    }
}