package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig

object NIDVersion {
    fun getSDKVersion(isRN: Boolean, isAdvancedDevice: Boolean,
                      nidBuildConfigWrapper: NIDBuildConfigWrapper = NIDBuildConfigWrapper()
    ): String {
        val rnText =
            if (isRN) {
                "-rn"
            } else {
                ""
            }

        val advText =
            if (isAdvancedDevice) {
                "-adv"
            } else {
                ""
            }

        return "5.android$rnText$advText-${nidBuildConfigWrapper.getBuildVersion()}"
    }

    fun getInternalCurrentVersion(isRN: Boolean, isAdvancedDevice: Boolean,
                                  nidBuildConfigWrapper: NIDBuildConfigWrapper = NIDBuildConfigWrapper()): String {
        return getSDKVersion(isRN, isAdvancedDevice) + " " + nidBuildConfigWrapper.getGitHash()
    }
}
