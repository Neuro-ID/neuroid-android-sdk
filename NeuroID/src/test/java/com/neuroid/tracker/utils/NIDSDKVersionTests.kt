package com.neuroid.tracker.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class NIDSDKVersionTests {

    @Test
    fun testSDKVersion_isRNtrue_isAdvancedDevicefalse() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        val version = NIDVersion.getSDKVersion(true, false, buildConfigWrapper)
        assert(version == "5.android-rn-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNfalse_isAdvancedDevicefalse() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        val version = NIDVersion.getSDKVersion(false, false, buildConfigWrapper)
        assert(version == "5.android-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNfalse_isAdvancedDevicetrue() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        val version = NIDVersion.getSDKVersion(false, true, buildConfigWrapper)
        assert(version == "5.android-adv-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNtrue_isAdvancedDevicetrue() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        val version = NIDVersion.getSDKVersion(true, true, buildConfigWrapper)
        assert(version == "5.android-rn-adv-yyyyyyy")
    }
}