package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VersionCheckerTest {

    private lateinit var mockSdkVersionProvider: NIDSdkVersionProvider

    @Before
    fun setup() {
        mockSdkVersionProvider = mockk()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // -----------------------------------------------------------------------
    // VersionChecker.isBuildVersionGreaterThanOrEqualTo31
    // -----------------------------------------------------------------------

    @Test
    fun test_isBuildVersionGreaterThanOrEqualTo31_whenSdkIsExactly31_returnsTrue() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.S
        val checker = VersionChecker(mockSdkVersionProvider)
        assertTrue(checker.isBuildVersionGreaterThanOrEqualTo31())
    }

    @Test
    fun test_isBuildVersionGreaterThanOrEqualTo31_whenSdkIsAbove31_returnsTrue() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.TIRAMISU
        val checker = VersionChecker(mockSdkVersionProvider)
        assertTrue(checker.isBuildVersionGreaterThanOrEqualTo31())
    }

    @Test
    fun test_isBuildVersionGreaterThanOrEqualTo31_whenSdkIsBelow31_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.R
        val checker = VersionChecker(mockSdkVersionProvider)
        assertFalse(checker.isBuildVersionGreaterThanOrEqualTo31())
    }

    @Test
    fun test_isBuildVersionGreaterThanOrEqualTo31_whenSdkIs24_returnsFalse() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.N
        val checker = VersionChecker(mockSdkVersionProvider)
        assertFalse(checker.isBuildVersionGreaterThanOrEqualTo31())
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — helper to build a mocked Context
    // -----------------------------------------------------------------------

    private fun createMockedContext(
        packageName: String,
        packageInfo: PackageInfo,
        minSdkVersion: Int = 24,
    ): Context {
        val appInfo = mockk<ApplicationInfo>(relaxed = true)
        appInfo.minSdkVersion = minSdkVersion

        val mockPackageManager = mockk<PackageManager>()
        every { mockPackageManager.getPackageInfo(packageName, 0) } returns packageInfo

        val mockContext = mockk<Context>()
        every { mockContext.packageName } returns packageName
        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.applicationInfo } returns appInfo

        return mockContext
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — API >= 28 (uses longVersionCode)
    // -----------------------------------------------------------------------

    @Test
    fun test_getAppMetaData_api28AndAbove_usesLongVersionCode() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val appInfo = ApplicationInfo()
        val packageInfo = mockk<PackageInfo>(relaxed = true)
        every { packageInfo.longVersionCode } returns 42L
        packageInfo.versionName = "2.0.0"
        packageInfo.packageName = "com.test.app"
        packageInfo.applicationInfo = appInfo

        val context = createMockedContext("com.test.app", packageInfo, minSdkVersion = 24)

        val result = getAppMetaData(context, "1.0.0-rn", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("2.0.0", result!!.versionName)
        assertEquals(42, result.versionNumber)
        assertEquals("com.test.app", result.packageName)
        assertEquals("1.0.0-rn", result.rnVersion)
        assertEquals(24, result.minOSVersion)
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — API < 28 (uses deprecated versionCode)
    // -----------------------------------------------------------------------

    @Test
    fun test_getAppMetaData_belowApi28_usesVersionCode() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.O

        val appInfo = ApplicationInfo()
        val packageInfo = PackageInfo().apply {
            versionName = "1.5.0"
            packageName = "com.test.legacy"
            @Suppress("DEPRECATION")
            versionCode = 7
            applicationInfo = appInfo
        }

        val context = createMockedContext("com.test.legacy", packageInfo, minSdkVersion = 21)

        val result = getAppMetaData(context, "", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("1.5.0", result!!.versionName)
        assertEquals(7, result.versionNumber)
        assertEquals("com.test.legacy", result.packageName)
        assertEquals(21, result.minOSVersion)
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — null-safe fallbacks
    // -----------------------------------------------------------------------

    @Test
    fun test_getAppMetaData_withNullVersionName_usesEmptyString() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val appInfo = ApplicationInfo()
        val packageInfo = PackageInfo().apply {
            versionName = null
            packageName = "com.test.app"
            longVersionCode = 1L
            applicationInfo = appInfo
        }

        val context = createMockedContext("com.test.app", packageInfo)

        val result = getAppMetaData(context, "", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("", result!!.versionName)
    }

    @Test
    fun test_getAppMetaData_withNullApplicationInfo_usesEmptyString() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val packageInfo = PackageInfo().apply {
            versionName = "1.0"
            packageName = "com.test.app"
            longVersionCode = 1L
            applicationInfo = null // name will be null
        }

        val context = createMockedContext("com.test.app", packageInfo)

        val result = getAppMetaData(context, "", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("", result!!.applicationName)
    }

    @Test
    fun test_getAppMetaData_withApplicationInfoName_returnsName() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val appInfo = ApplicationInfo().apply { name = "MyApp" }
        val packageInfo = PackageInfo().apply {
            versionName = "1.0"
            packageName = "com.test.app"
            longVersionCode = 1L
            applicationInfo = appInfo
        }

        val context = createMockedContext("com.test.app", packageInfo)

        val result = getAppMetaData(context, "", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("MyApp", result!!.applicationName)
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — rnVersion pass-through
    // -----------------------------------------------------------------------

    @Test
    fun test_getAppMetaData_passesRnVersionThrough() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val appInfo = ApplicationInfo()
        val packageInfo = PackageInfo().apply {
            versionName = "3.0.0"
            packageName = "com.neuroid.tracker"
            longVersionCode = 100L
            applicationInfo = appInfo
        }

        val context = createMockedContext("com.neuroid.tracker", packageInfo)

        val result = getAppMetaData(context, "rn-2.0", mockSdkVersionProvider)

        assertNotNull(result)
        assertEquals("rn-2.0", result!!.rnVersion)
    }

    // -----------------------------------------------------------------------
    // getAppMetaData — failure path
    // -----------------------------------------------------------------------

    @Test
    fun test_getAppMetaData_whenPackageNotFound_returnsNull() {
        every { mockSdkVersionProvider.getSdkInt() } returns Build.VERSION_CODES.P

        val mockPackageManager = mockk<PackageManager>()
        every {
            mockPackageManager.getPackageInfo(any<String>(), any<Int>())
        } throws PackageManager.NameNotFoundException("Package not found")

        val mockContext = mockk<Context>()
        every { mockContext.packageName } returns "com.missing.app"
        every { mockContext.packageManager } returns mockPackageManager

        val result = getAppMetaData(mockContext, "", mockSdkVersionProvider)

        assertNull(result)
    }
}


