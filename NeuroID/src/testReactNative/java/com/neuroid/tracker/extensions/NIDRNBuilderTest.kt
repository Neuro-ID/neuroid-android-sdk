package com.neuroid.tracker.extensions

import android.app.Application
import com.facebook.react.bridge.ReadableMap
import com.neuroid.tracker.NeuroID
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class NIDRNBuilderTest {
    @Test
    fun testRNOption_no_options_set() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every {options.hasKey(any())} returns false
        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)
        println(mapOptions)
        assert(!(mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean))
        assert((mapOptions[RNConfigOptions.environment] as String) == NeuroID.PRODUCTION)
        assert((mapOptions[RNConfigOptions.advancedDeviceKey] as String) == "")
        assert(!(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean))
        assert((mapOptions[RNConfigOptions.hostReactNativeVersion] as String) == "")
    }

    @Test
    fun testRNOption_environment_isAdvancedDevice_options_set() {
        val mockApp = mockk<Application>()
        val options = mockk<ReadableMap>()
        every {options.hasKey(RNConfigOptions.isAdvancedDevice.name)} returns true
        every {options.hasKey(RNConfigOptions.environment.name)} returns true
        every {options.hasKey(RNConfigOptions.advancedDeviceKey.name)} returns true
        every {options.hasKey(RNConfigOptions.useAdvancedDeviceProxy.name)} returns true
        every {options.hasKey( RNConfigOptions.hostReactNativeVersion.name)} returns true
        every {options.getBoolean(RNConfigOptions.isAdvancedDevice.name)} returns true
        every {options.getString(RNConfigOptions.advancedDeviceKey.name)} returns "testkey"
        every {options.getBoolean(RNConfigOptions.useAdvancedDeviceProxy.name)} returns false
        every {options.getString(RNConfigOptions.environment.name)} returns NeuroID.PRODSCRIPT_DEVCOLLECTION
        every {options.getString(RNConfigOptions.hostReactNativeVersion.name)} returns "0.71.0"
        val t = NIDRNBuilder(mockApp, "dummy_key", options)
        val mapOptions = t.parseOptions(options)
        println(mapOptions)
        assert((mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean))
        assert((mapOptions[RNConfigOptions.environment] as String) == NeuroID.PRODSCRIPT_DEVCOLLECTION)
        assert((mapOptions[RNConfigOptions.advancedDeviceKey] as String) == "testkey")
        assert(!(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean))
        assert(mapOptions[RNConfigOptions.hostReactNativeVersion] as String == "0.71.0")
    }

    @Test
    fun testRNOption_environment_null_options() {
        val mockApp = mockk<Application>()
        val t = NIDRNBuilder(mockApp, "dummy_key", null)
        val mapOptions = t.parseOptions(null)
        println(mapOptions)
        assert(!(mapOptions[RNConfigOptions.isAdvancedDevice] as Boolean))
        assert((mapOptions[RNConfigOptions.environment] as String) == NeuroID.PRODUCTION)
        assert((mapOptions[RNConfigOptions.advancedDeviceKey] as String) == "")
        assert(!(mapOptions[RNConfigOptions.useAdvancedDeviceProxy] as Boolean))
        assert((mapOptions[RNConfigOptions.hostReactNativeVersion] as String) == "")
    }
}