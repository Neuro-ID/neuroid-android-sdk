package com.neuroid.tracker.extensions

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.react.bridge.ReadableMap
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDConfiguration
import com.neuroid.tracker.models.NIDRegion

class NIDRNBuilder(
    val application: Application? = null,
    val clientKey: String = "",
    private val rnOptions: ReadableMap? = null,
) {
    fun build() {
        val options = parseOptions(rnOptions)
        Log.d("NIDRNBuilder", "set options: $options")
        NeuroID.BuilderConfig(
            application,
            NIDConfiguration(
                clientKey = clientKey,
                isAdvancedDevice = options[RNConfigOptions.isAdvancedDevice] as Boolean,
                advancedDeviceKey = options[RNConfigOptions.advancedDeviceKey] as String,
                useAdvancedDeviceProxy = options[RNConfigOptions.useAdvancedDeviceProxy] as Boolean,
                region = NIDRegion.valueOf(options[RNConfigOptions.region] as String),
            ),
        ).build()

        NeuroID.getInternalInstance()?.setIsRN(options[RNConfigOptions.rnVersion] as String)
    }

    /**
     * Returns a guaranteed map of options (environment and isAdvancedDevice) that we can use
     * to configure NeuroID properly.
     */
    @VisibleForTesting
    internal fun parseOptions(rnOptions: ReadableMap?): Map<RNConfigOptions, Any> {
        // defaults
        var isAdvancedDevice = false
        var advancedDeviceKey = ""
        var useAdvancedDeviceProxy = true
        var rnVersion = ""
        var region = NIDRegion.US_WEST_DEFAULT.name

        val options = mutableMapOf<RNConfigOptions, Any>()
        rnOptions?.let { rnOptionsMap ->
            // set the advanced device key from the advancedDeviceKey option, default empty string
            if (rnOptionsMap.hasKey(RNConfigOptions.advancedDeviceKey.name)) {
                rnOptionsMap.getString(RNConfigOptions.advancedDeviceKey.name)?.let {
                    advancedDeviceKey = it
                }
            }
            // set the useAdvancedDeviceProxy flag true or false from useAdvancedDeviceProxy option, default false
            if (rnOptionsMap.hasKey(RNConfigOptions.useAdvancedDeviceProxy.name)) {
                rnOptionsMap.getBoolean(RNConfigOptions.useAdvancedDeviceProxy.name).let {
                    useAdvancedDeviceProxy = it
                }
            }
            // set the is advanced flag true or false from isAdvancedDevice option, default false
            if (rnOptionsMap.hasKey(RNConfigOptions.isAdvancedDevice.name)) {
                rnOptionsMap.getBoolean(RNConfigOptions.isAdvancedDevice.name).let {
                    isAdvancedDevice = it
                }
            }
            // set the host reactnative version from rnVersion option, default ""
            if (rnOptionsMap.hasKey(RNConfigOptions.rnVersion.name)) {
                rnOptionsMap.getString(RNConfigOptions.rnVersion.name)?.let {
                    rnVersion = it
                }
            }
            // add more regions here, default to US_WEST_DEFAULT if we don't get a matching region from the options
            // usWest will default to US_WEST_DEFAULT, but we will log a warning to the console that it is deprecated
            // and will be removed in the future
            if (rnOptionsMap.hasKey(RNConfigOptions.region.name)) {
                rnOptionsMap.getString(RNConfigOptions.region.name)?.let {
                    when (it) {
                        NIDRegion.US_WEST.name -> region = NIDRegion.US_WEST.name
                        NIDRegion.US_EAST.name -> region = NIDRegion.US_EAST.name
                        else -> region = NIDRegion.US_WEST_DEFAULT.name
                    }
                }
            }
        }
        options[RNConfigOptions.isAdvancedDevice] = isAdvancedDevice
        options[RNConfigOptions.advancedDeviceKey] = advancedDeviceKey
        options[RNConfigOptions.useAdvancedDeviceProxy] = useAdvancedDeviceProxy
        options[RNConfigOptions.rnVersion] = rnVersion
        options[RNConfigOptions.region] = region
        return options
    }
}

@Suppress("ktlint:standard:enum-entry-name-case")
enum class RNConfigOptions {
    isAdvancedDevice,
    environment,
    advancedDeviceKey,
    useAdvancedDeviceProxy,
    rnVersion,
    region,
}
