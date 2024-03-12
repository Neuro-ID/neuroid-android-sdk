package com.neuroid.tracker.utils

import android.os.Build

class VersionChecker() {
    /**
     * Returns Boolean to indicate if device build version is > 31
     */
    fun isBuildVersionGreaterThan31(): Boolean  {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S
    }
}
