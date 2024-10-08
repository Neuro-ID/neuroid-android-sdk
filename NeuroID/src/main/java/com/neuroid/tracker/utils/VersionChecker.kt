package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.neuroid.tracker.models.ApplicationMetaData

class VersionChecker() {
    /**
     * Returns Boolean to indicate if device build version is >= 31
     */
    fun isBuildVersionGreaterThanOrEqualTo31(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}

fun getAppMetaData(context: Context): ApplicationMetaData? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }

        ApplicationMetaData(
            versionName = packageInfo.versionName,
            versionNumber = versionCode,
            packageName = packageInfo.packageName,
            applicationName = packageInfo.applicationInfo.name?:"",
        )
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}
