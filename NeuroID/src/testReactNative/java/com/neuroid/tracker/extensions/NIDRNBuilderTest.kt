package com.neuroid.tracker.extensions

import android.app.Application
import com.facebook.react.bridge.ReadableMap
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDRegion
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NIDRNBuilderTest {
    @Test
    fun testRNOption_no_options_set() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every {options.hasKey(any())} returns false
        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)
        assertFalse(mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean)
        assertEquals(NeuroID.PRODUCTION, mapOptions[RNConfigOptions.environment] as String)
        assertEquals("", mapOptions[RNConfigOptions.advancedDeviceKey] as String)
        assertTrue(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean)
        assertEquals("", mapOptions[RNConfigOptions.rnVersion] as String)
        assertEquals(NIDRegion.usWest.name, mapOptions[RNConfigOptions.region] as String)
    }

    @Test
    fun testRNOption_environment_isAdvancedDevice_options_set() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every {options.hasKey(RNConfigOptions.isAdvancedDevice.name)} returns true
        every {options.hasKey(RNConfigOptions.environment.name)} returns true
        every {options.hasKey(RNConfigOptions.advancedDeviceKey.name)} returns true
        every {options.hasKey(RNConfigOptions.useAdvancedDeviceProxy.name)} returns true
        every {options.hasKey( RNConfigOptions.rnVersion.name)} returns true
        every {options.hasKey(RNConfigOptions.region.name)} returns true
        every {options.getBoolean(RNConfigOptions.isAdvancedDevice.name)} returns true
        every {options.getString(RNConfigOptions.advancedDeviceKey.name)} returns "testkey"
        every {options.getBoolean(RNConfigOptions.useAdvancedDeviceProxy.name)} returns false
        every {options.getString(RNConfigOptions.environment.name)} returns NeuroID.PRODSCRIPT_DEVCOLLECTION
        every {options.getString(RNConfigOptions.rnVersion.name)} returns "0.71.0"
        every {options.getString(RNConfigOptions.region.name)} returns NIDRegion.usWest.name
        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)
        assertTrue(mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean)
        assertEquals(NeuroID.PRODSCRIPT_DEVCOLLECTION, mapOptions[RNConfigOptions.environment] as String)
        assertEquals("testkey", mapOptions[RNConfigOptions.advancedDeviceKey] as String)
        assertFalse(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean)
        assertEquals("0.71.0", mapOptions[RNConfigOptions.rnVersion] as String)
        assertEquals(NIDRegion.usWest.name, mapOptions[RNConfigOptions.region] as String)
    }

    @Test
    fun testRNOption_environment_null_options() {
        val mockApp = mockk<Application>()
        val t = NIDRNBuilder(mockApp, "dummy_key", null)
        val mapOptions = t.parseOptions(null)
        assertFalse(mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean)
        assertEquals(NeuroID.PRODUCTION, mapOptions[RNConfigOptions.environment] as String)
        assertEquals("", mapOptions[RNConfigOptions.advancedDeviceKey] as String)
        assertTrue(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean)
        assertEquals("", mapOptions[RNConfigOptions.rnVersion] as String)
        assertEquals(NIDRegion.usWest.name, mapOptions[RNConfigOptions.region] as String)
    }

    @Test
    fun testRNOption_environment_development_option_set() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every { options.hasKey(any()) } returns false
        every { options.hasKey(RNConfigOptions.environment.name) } returns true
        every { options.getString(RNConfigOptions.environment.name) } returns NeuroID.DEVELOPMENT

        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)

        assertEquals(NeuroID.DEVELOPMENT, mapOptions[RNConfigOptions.environment] as String)
    }

    @Test
    fun testRNOption_invalid_region_falls_back_to_usWest() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every { options.hasKey(any()) } returns false
        every { options.hasKey(RNConfigOptions.region.name) } returns true
        every { options.getString(RNConfigOptions.region.name) } returns "invalid-region"

        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)

        assertEquals(NIDRegion.usWest.name, mapOptions[RNConfigOptions.region] as String)
    }
}

