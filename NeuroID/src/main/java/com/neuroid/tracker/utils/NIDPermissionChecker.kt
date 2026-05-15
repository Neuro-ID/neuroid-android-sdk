package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Wrapper around [ActivityCompat.checkSelfPermission] to allow easy mocking in tests
 * without reflection or Robolectric. Follows the same pattern as [NIDSdkVersionProvider].
 */
open class NIDPermissionChecker {
    /**
     * Returns [PackageManager.PERMISSION_GRANTED] or [PackageManager.PERMISSION_DENIED].
     */
    open fun checkSelfPermission(context: Context, permission: String): Int =
        ActivityCompat.checkSelfPermission(context, permission)
}

