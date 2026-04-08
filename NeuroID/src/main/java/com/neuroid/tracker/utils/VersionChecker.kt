package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.neuroid.tracker.models.ApplicationMetaData

class VersionChecker(
    private val sdkVersionProvider: NIDSdkVersionProvider = NIDSdkVersionProvider(),
) {
    /**
     * Returns Boolean to indicate if device build version is >= 31
     */
    fun isBuildVersionGreaterThanOrEqualTo31(): Boolean {
        return sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.S
    }
}

fun getAppMetaData(context: Context,
                   rnVersion: String,
                   sdkVersionProvider: NIDSdkVersionProvider = NIDSdkVersionProvider()): ApplicationMetaData? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val versionCode =
            if (sdkVersionProvider.getSdkInt() >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }

        ApplicationMetaData(
            versionName = packageInfo.versionName?:"",
            versionNumber = versionCode,
            packageName = packageInfo.packageName,
            applicationName = packageInfo.applicationInfo?.name?:"",
            rnVersion = rnVersion,
            minOSVersion = context.applicationInfo.minSdkVersion,
        )
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}
