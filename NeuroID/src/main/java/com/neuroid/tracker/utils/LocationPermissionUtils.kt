package com.neuroid.tracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class LocationPermissionUtils {
    fun isNotAllowedToCollectLocations(context: Context): Boolean {
        val fineResult =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )

        val coarseResult =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        return (
            fineResult != PackageManager.PERMISSION_GRANTED &&
                coarseResult != PackageManager.PERMISSION_GRANTED
        )
    }
} 
