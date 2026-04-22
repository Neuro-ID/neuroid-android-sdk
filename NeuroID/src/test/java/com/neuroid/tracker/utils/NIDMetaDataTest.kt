package com.neuroid.tracker.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import com.neuroid.tracker.service.LocationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NIDMetaDataTest {

    @Before
    fun setup() {
        // Mock RootHelper constructor so that Build.* null values don't cause NPEs
        mockkConstructor(RootHelper::class)
        every { anyConstructed<RootHelper>().isRooted(any()) } returns false
        every { anyConstructed<RootHelper>().isProbablyEmulator() } returns false
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // -----------------------------------------------------------------------
    // Helper: create a fully mocked Context for NIDMetaData construction
    // -----------------------------------------------------------------------

    private fun createMockedContext(
        widthPixels: Int = 1080,
        heightPixels: Int = 1920,
        carrierName: String = "T-Mobile",
        totalMemBytes: Long = 4L * 1024 * 1024 * 1024, // 4 GB
        batteryCapacity: Int = 85,
        wifiEnabled: Boolean? = true,
        firstInstallTime: Long = 1_700_000_000_000L,
        throwOnTelephony: Boolean = false,
        throwOnActivityManager: Boolean = false,
        throwOnBattery: Boolean = false,
        throwOnWifi: Boolean = false,
        throwOnResources: Boolean = false,
    ): Context {
        val context = mockk<Context>(relaxed = true)

        // Package info / install time
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.test.app"
        val packageInfo = PackageInfo()
        packageInfo.firstInstallTime = firstInstallTime
        every { packageManager.getPackageInfo("com.test.app", 0) } returns packageInfo
        // RootHelper.isAnyPackageFromListInstalled calls getPackageInfo for many packages —
        // throw NameNotFoundException for all non-app packages by default (meaning not rooted)
        every { packageManager.getPackageInfo(neq("com.test.app"), any<Int>()) } throws
            PackageManager.NameNotFoundException("Not found")

        // Display metrics / screen resolution
        val resources = mockk<Resources>()
        val displayMetrics = DisplayMetrics()
        displayMetrics.widthPixels = widthPixels
        displayMetrics.heightPixels = heightPixels
        if (throwOnResources) {
            every { context.resources } throws RuntimeException("No resources")
        } else {
            every { context.resources } returns resources
            every { resources.displayMetrics } returns displayMetrics
        }

        // TelephonyManager / carrier
        if (throwOnTelephony) {
            every { context.getSystemService(Context.TELEPHONY_SERVICE) } throws
                SecurityException("No telephony permission")
        } else {
            val telephonyManager = mockk<TelephonyManager>()
            every { context.getSystemService(Context.TELEPHONY_SERVICE) } returns telephonyManager
            every { telephonyManager.networkOperatorName } returns carrierName
        }

        // ActivityManager / memory
        if (throwOnActivityManager) {
            every { context.getSystemService(Context.ACTIVITY_SERVICE) } throws
                RuntimeException("No activity service")
        } else {
            val activityManager = mockk<ActivityManager>()
            every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
            every { activityManager.getMemoryInfo(any()) } answers {
                val memInfo = firstArg<ActivityManager.MemoryInfo>()
                val field = ActivityManager.MemoryInfo::class.java.getDeclaredField("totalMem")
                field.isAccessible = true
                field.setLong(memInfo, totalMemBytes)
            }
        }

        // BatteryManager / battery level
        if (throwOnBattery) {
            every { context.getSystemService(Context.BATTERY_SERVICE) } throws
                RuntimeException("No battery service")
        } else {
            val batteryManager = mockk<BatteryManager>()
            every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
            every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns batteryCapacity
        }

        // WifiManager / wifi status
        if (throwOnWifi) {
            every { context.getSystemService(Context.WIFI_SERVICE) } throws
                SecurityException("No wifi permission")
        } else {
            val wifiManager = mockk<WifiManager>()
            every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
            if (wifiEnabled != null) {
                every { wifiManager.isWifiEnabled } returns wifiEnabled
            } else {
                every { wifiManager.isWifiEnabled } throws SecurityException("No permission")
            }
        }

        return context
    }

    // -----------------------------------------------------------------------
    // Construction — happy path
    // -----------------------------------------------------------------------

    @Test
    fun test_construction_happyPath_setsAllFields() {
        val context = createMockedContext()
        val sdkVersionProvider = mockk<NIDSdkVersionProvider>()
        every { sdkVersionProvider.getSdkInt() } returns 33

        val metaData = NIDMetaData(context, sdkVersionProvider)
        val json = metaData.toJson()

        assertEquals("1080,1920", json.getString("displayResolution"))
        assertEquals("T-Mobile", json.getString("carrier"))
        assertEquals(85, json.getInt("batteryLevel"))
        assertEquals("33", json.getString("osVersion"))
        // Build.* fields may be null in unit test env — validate JSON is well-formed
        assertNotNull(json)
        assertTrue(json.has("totalMemory"))
        assertTrue(json.has("isJailBreak"))
        assertTrue(json.has("isWifiOn"))
        assertTrue(json.has("isSimulator"))
        assertTrue(json.has("gpsCoordinates"))
        assertTrue(json.has("lastInstallTime"))
    }

    // -----------------------------------------------------------------------
    // toJson — validates all expected keys
    // -----------------------------------------------------------------------

    @Test
    fun test_toJson_containsAllExpectedKeys() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        // Keys that are always present regardless of Build.* being null
        val alwaysExpectedKeys = listOf(
            "displayResolution", "osVersion",
            "carrier", "totalMemory", "batteryLevel", "isJailBreak",
            "isWifiOn", "isSimulator", "gpsCoordinates", "lastInstallTime",
        )

        alwaysExpectedKeys.forEach { key ->
            assertTrue("Missing key: $key", json.has(key))
        }

        // Build.* fields (brand, device, display, manufacturer, model, product)
        // may be null in unit test env, but toJson() should still not crash
        assertNotNull(json)
        assertTrue(json.length() >= alwaysExpectedKeys.size)
    }

    @Test
    fun test_toJson_gpsCoordinates_hasExpectedStructure() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        val gps = json.getJSONObject("gpsCoordinates")
        assertEquals(-1.0, gps.getDouble("latitude"), 0.001)
        assertEquals(-1.0, gps.getDouble("longitude"), 0.001)
        assertEquals("unknown", gps.getString("authorizationStatus"))
    }

    // -----------------------------------------------------------------------
    // toString — delegates to toJson
    // -----------------------------------------------------------------------

    @Test
    fun test_toString_returnsJsonString() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)

        val str = metaData.toString()
        // Should be parseable JSON
        val json = JSONObject(str)
        assertTrue(json.has("totalMemory"))
        assertTrue(json.has("batteryLevel"))
    }

    // -----------------------------------------------------------------------
    // Screen resolution — exception fallback
    // -----------------------------------------------------------------------

    @Test
    fun test_getScreenResolution_whenExceptionThrown_returnsEmpty() {
        val context = createMockedContext(throwOnResources = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals("", json.getString("displayResolution"))
    }

    @Test
    fun test_getScreenResolution_differentResolutions() {
        val context = createMockedContext(widthPixels = 2560, heightPixels = 1440)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals("2560,1440", json.getString("displayResolution"))
    }

    // -----------------------------------------------------------------------
    // Carrier — exception fallback
    // -----------------------------------------------------------------------

    @Test
    fun test_getCarrierName_whenExceptionThrown_returnsEmpty() {
        val context = createMockedContext(throwOnTelephony = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals("", json.getString("carrier"))
    }

    @Test
    fun test_getCarrierName_returnsOperatorName() {
        val context = createMockedContext(carrierName = "Verizon")
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals("Verizon", json.getString("carrier"))
    }

    // -----------------------------------------------------------------------
    // Memory — exception fallback
    // -----------------------------------------------------------------------

    @Test
    fun test_getMemory_whenExceptionThrown_returnsZero() {
        val context = createMockedContext(throwOnActivityManager = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals(0.0, json.getDouble("totalMemory"), 0.001)
    }

    @Test
    fun test_getMemory_calculatesCorrectly() {
        // 4 GB in bytes
        val fourGBBytes = 4L * 1024 * 1024 * 1024
        val context = createMockedContext(totalMemBytes = fourGBBytes)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals(4.0, json.getDouble("totalMemory"), 0.01)
    }

    // -----------------------------------------------------------------------
    // Battery level — exception fallback
    // -----------------------------------------------------------------------

    @Test
    fun test_getBatteryLevel_whenExceptionThrown_returnsZero() {
        val context = createMockedContext(throwOnBattery = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals(0, json.getInt("batteryLevel"))
    }

    @Test
    fun test_getBatteryLevel_returnsCapacity() {
        val context = createMockedContext(batteryCapacity = 42)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals(42, json.getInt("batteryLevel"))
    }

    // -----------------------------------------------------------------------
    // Wifi status — exception fallback
    // -----------------------------------------------------------------------

    @Test
    fun test_getWifiStatus_whenExceptionThrown_returnsNull() {
        val context = createMockedContext(throwOnWifi = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        // When wifi throws, isWifiOn should be JSONObject.NULL
        assertTrue(json.isNull("isWifiOn"))
    }

    @Test
    fun test_getWifiStatus_whenEnabled_returnsTrue() {
        val context = createMockedContext(wifiEnabled = true)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertTrue(json.getBoolean("isWifiOn"))
    }

    @Test
    fun test_getWifiStatus_whenDisabled_returnsFalse() {
        val context = createMockedContext(wifiEnabled = false)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertFalse(json.getBoolean("isWifiOn"))
    }

    @Test
    fun test_getWifiStatus_whenPermissionDenied_returnsNull() {
        val context = createMockedContext(wifiEnabled = null)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertTrue(json.isNull("isWifiOn"))
    }

    // -----------------------------------------------------------------------
    // lastInstallTime
    // -----------------------------------------------------------------------

    @Test
    fun test_lastInstallTime_returnsCorrectValue() {
        val context = createMockedContext(firstInstallTime = 1_700_000_000_000L)
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertEquals(1_700_000_000_000L, json.getLong("lastInstallTime"))
    }

    @Test
    fun test_lastInstallTime_whenPackageManagerNull_returnsDefault() {
        val context = mockk<Context>(relaxed = true)
        every { context.packageManager } returns null

        // Set up remaining mocks so construction doesn't fail
        val resources = mockk<Resources>()
        val displayMetrics = DisplayMetrics()
        displayMetrics.widthPixels = 1080
        displayMetrics.heightPixels = 1920
        every { context.resources } returns resources
        every { resources.displayMetrics } returns displayMetrics

        val telephonyManager = mockk<TelephonyManager>()
        every { context.getSystemService(Context.TELEPHONY_SERVICE) } returns telephonyManager
        every { telephonyManager.networkOperatorName } returns "Test"

        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } answers {
            val memInfo = firstArg<ActivityManager.MemoryInfo>()
            val field = ActivityManager.MemoryInfo::class.java.getDeclaredField("totalMem")
            field.isAccessible = true
            field.setLong(memInfo, 4L * 1024 * 1024 * 1024)
        }

        val batteryManager = mockk<BatteryManager>()
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 50

        val wifiManager = mockk<WifiManager>()
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.isWifiEnabled } returns true

        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        // When packageManager is null, lastInstallTime defaults to -1
        assertEquals(-1L, json.getLong("lastInstallTime"))
    }

    // -----------------------------------------------------------------------
    // osVersion — uses sdkVersionProvider
    // -----------------------------------------------------------------------

    @Test
    fun test_osVersion_reflectsSdkVersionProvider() {
        val context = createMockedContext()
        val sdkVersionProvider = mockk<NIDSdkVersionProvider>()
        every { sdkVersionProvider.getSdkInt() } returns 34

        val metaData = NIDMetaData(context, sdkVersionProvider)
        val json = metaData.toJson()

        assertEquals("34", json.getString("osVersion"))
    }

    // -----------------------------------------------------------------------
    // isJailBreak / isSimulator — values from RootHelper
    // -----------------------------------------------------------------------

    @Test
    fun test_isJailBreak_whenRooted_returnsTrue() {
        every { anyConstructed<RootHelper>().isRooted(any()) } returns true

        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertTrue(json.getBoolean("isJailBreak"))
    }

    @Test
    fun test_isJailBreak_whenNotRooted_returnsFalse() {
        every { anyConstructed<RootHelper>().isRooted(any()) } returns false

        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertFalse(json.getBoolean("isJailBreak"))
    }

    @Test
    fun test_isSimulator_whenEmulator_returnsTrue() {
        every { anyConstructed<RootHelper>().isProbablyEmulator() } returns true

        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertTrue(json.getBoolean("isSimulator"))
    }

    @Test
    fun test_isSimulator_whenNotEmulator_returnsFalse() {
        every { anyConstructed<RootHelper>().isProbablyEmulator() } returns false

        val context = createMockedContext()
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        assertFalse(json.getBoolean("isSimulator"))
    }

    // -----------------------------------------------------------------------
    // getLastKnownLocation
    // -----------------------------------------------------------------------

    @Test
    fun test_getLastKnownLocation_delegatesToLocationService() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)

        val locationManager = mockk<LocationManager>()
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager

        val locationService = mockk<LocationService>(relaxed = true)

        metaData.getLastKnownLocation(context, isLocationAllowed = true, locationService = locationService)

        verify {
            locationService.getLastKnownLocation(
                context,
                any(),
                locationManager = locationManager,
                isLocationAllowed = true,
            )
        }
    }

    @Test
    fun test_getLastKnownLocation_whenLocationServiceNull_doesNotCrash() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)

        // Should not throw when locationService is null
        metaData.getLastKnownLocation(context, isLocationAllowed = true, locationService = null)
    }

    @Test
    fun test_getLastKnownLocation_passesIsLocationAllowedFalse() {
        val context = createMockedContext()
        val metaData = NIDMetaData(context)

        val locationManager = mockk<LocationManager>()
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager

        val locationService = mockk<LocationService>(relaxed = true)

        metaData.getLastKnownLocation(context, isLocationAllowed = false, locationService = locationService)

        verify {
            locationService.getLastKnownLocation(
                context,
                any(),
                locationManager = locationManager,
                isLocationAllowed = false,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Companion constants
    // -----------------------------------------------------------------------

    @Test
    fun test_companionConstants_haveExpectedValues() {
        assertEquals("denied", NIDMetaData.LOCATION_DENIED)
        assertEquals("unknown", NIDMetaData.LOCATION_UNKNOWN)
        assertEquals("authorized", NIDMetaData.LOCATION_AUTHORIZED_ALWAYS)
    }

    // -----------------------------------------------------------------------
    // Default sdkVersionProvider
    // -----------------------------------------------------------------------

    @Test
    fun test_defaultSdkVersionProvider_usesRealValue() {
        val context = createMockedContext()
        // Construct with default sdkVersionProvider
        val metaData = NIDMetaData(context)
        val json = metaData.toJson()

        // osVersion should be a parseable integer string
        val osVersion = json.getString("osVersion")
        assertNotNull(osVersion)
        assertTrue(osVersion.toInt() >= 0)
    }
}





