package com.neuroid.tracker.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NIDSDKVersionTests {

    @Test
    fun testSDKVersion_isRNtrue_isAdvancedDevicefalse() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        every {buildConfigWrapper.getFlavor()} returns "reactNativeLib"
        val version = NIDVersion.getSDKVersion(buildConfigWrapper)
        assert(version == "5.android-rn-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNfalse_isAdvancedDevicefalse() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        every {buildConfigWrapper.getFlavor()} returns "androidLib"
        val version = NIDVersion.getSDKVersion(buildConfigWrapper)
        assert(version == "5.android-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNfalse_isAdvancedDevicetrue() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        every {buildConfigWrapper.getFlavor()} returns "androidAdvancedDeviceLib"
        val version = NIDVersion.getSDKVersion(buildConfigWrapper)
        assert(version == "5.android-adv-yyyyyyy")
    }

    @Test
    fun testSDKVersion_isRNtrue_isAdvancedDevicetrue() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every {buildConfigWrapper.getBuildVersion()} returns "yyyyyyy"
        every {buildConfigWrapper.getFlavor()} returns "reactNativeAdvancedDeviceLib"
        val version = NIDVersion.getSDKVersion(buildConfigWrapper)
        assert(version == "5.android-rn-adv-yyyyyyy")
    }

    // -----------------------------------------------------------------------
    // getInternalCurrentVersion tests
    // -----------------------------------------------------------------------

    @Test
    fun testInternalCurrentVersion_androidLib() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "1.2.3"
        every { buildConfigWrapper.getFlavor() } returns "androidLib"
        every { buildConfigWrapper.getGitHash() } returns "abc1234"

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        assertEquals("5.android-1.2.3 abc1234", version)
    }

    @Test
    fun testInternalCurrentVersion_reactNativeLib() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "2.0.0"
        every { buildConfigWrapper.getFlavor() } returns "reactNativeLib"
        every { buildConfigWrapper.getGitHash() } returns "def5678"

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        assertEquals("5.android-rn-2.0.0 def5678", version)
    }

    @Test
    fun testInternalCurrentVersion_advancedDevice() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "3.0.0"
        every { buildConfigWrapper.getFlavor() } returns "androidAdvancedDeviceLib"
        every { buildConfigWrapper.getGitHash() } returns "ghi9012"

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        assertEquals("5.android-adv-3.0.0 ghi9012", version)
    }

    @Test
    fun testInternalCurrentVersion_reactNativeAdvancedDevice() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "4.0.0"
        every { buildConfigWrapper.getFlavor() } returns "reactNativeAdvancedDeviceLib"
        every { buildConfigWrapper.getGitHash() } returns "jkl3456"

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        assertEquals("5.android-rn-adv-4.0.0 jkl3456", version)
    }

    @Test
    fun testInternalCurrentVersion_includesSpaceBetweenVersionAndHash() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "1.0.0"
        every { buildConfigWrapper.getFlavor() } returns "androidLib"
        every { buildConfigWrapper.getGitHash() } returns "xyz"

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        // Verify format: "<sdkVersion> <gitHash>"
        assert(version.contains(" "))
        val parts = version.split(" ")
        assertEquals(2, parts.size)
        assertEquals("5.android-1.0.0", parts[0])
        assertEquals("xyz", parts[1])
    }

    @Test
    fun testInternalCurrentVersion_emptyGitHash() {
        val buildConfigWrapper = mockk<NIDBuildConfigWrapper>()
        every { buildConfigWrapper.getBuildVersion() } returns "1.0.0"
        every { buildConfigWrapper.getFlavor() } returns "androidLib"
        every { buildConfigWrapper.getGitHash() } returns ""

        val version = NIDVersion.getInternalCurrentVersion(buildConfigWrapper)

        assertEquals("5.android-1.0.0 ", version)
    }

    @Test
    fun testInternalCurrentVersion_defaultBuildConfigWrapper() {
        // Call with default parameter to cover the $default bridge method
        val version = NIDVersion.getInternalCurrentVersion()

        // Should return a non-empty string with the default BuildConfig values
        assert(version.isNotEmpty())
        assert(version.startsWith("5.android"))
        assert(version.contains(" "))
    }

    @Test
    fun testSDKVersion_defaultBuildConfigWrapper() {
        // Call with default parameter to cover the $default bridge method
        val version = NIDVersion.getSDKVersion()

        assert(version.isNotEmpty())
        assert(version.startsWith("5.android"))
    }
}