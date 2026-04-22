package com.neuroid.tracker.utils

import android.os.Build

/**
 * Wrapper around [Build.VERSION.SDK_INT] to allow easy mocking in tests
 * without reflection. Follows the same pattern as [NIDBuildConfigWrapper].
 */
open class NIDSdkVersionProvider {
    /**
     * Returns the current device SDK version (e.g. [Build.VERSION_CODES.TIRAMISU]).
     */
    open fun getSdkInt(): Int = Build.VERSION.SDK_INT
}

