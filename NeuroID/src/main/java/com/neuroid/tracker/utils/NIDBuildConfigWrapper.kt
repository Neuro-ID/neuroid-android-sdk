package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig

/**
 *  required for testing, cannot mockkStatic BuildConfig.
 */
class NIDBuildConfigWrapper {
    fun getBuildVersion(): String = BuildConfig.VERSION_NAME

    fun getGitHash(): String = BuildConfig.GIT_HASH

    fun getFlavor(): String = BuildConfig.FLAVOR
}
