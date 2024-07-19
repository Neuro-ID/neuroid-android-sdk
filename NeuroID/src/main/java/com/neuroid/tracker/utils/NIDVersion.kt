package com.neuroid.tracker.utils

object NIDVersion {
    internal fun getSDKVersion(
        nidBuildConfigWrapper: NIDBuildConfigWrapper = NIDBuildConfigWrapper(),
    ): String {
        val rnText =
            if (nidBuildConfigWrapper.getFlavor().lowercase().contains("react")) {
                "-rn"
            } else {
                ""
            }

        val advText =
            if (nidBuildConfigWrapper.getFlavor().lowercase().contains("advanceddevice")) {
                "-adv"
            } else {
                ""
            }

        return "5.android$rnText$advText-${nidBuildConfigWrapper.getBuildVersion()}"
    }

    internal fun getInternalCurrentVersion(
        nidBuildConfigWrapper: NIDBuildConfigWrapper = NIDBuildConfigWrapper(),
    ): String {
        return getSDKVersion(nidBuildConfigWrapper) + " " + nidBuildConfigWrapper.getGitHash()
    }
}
