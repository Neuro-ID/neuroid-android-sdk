package com.neuroid.tracker.extensions

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.react.bridge.ReadableMap
import com.neuroid.tracker.NeuroID

class NIDRNBuilder( val application: Application? = null,
                    val clientKey: String = "",
                    private val rnOptions: ReadableMap? = null) {
    fun build() {
        val options = parseOptions(rnOptions)
        // call NeuroID builder to initialize NeuroID with the options.
        Log.d("NIDRNBuilder", "set options: $options")
        NeuroID.Builder(application, clientKey,
            options[RNConfigOptions.isAdvancedDevice] as Boolean,
            options[RNConfigOptions.environment] as String).build()
        NeuroID.getInstance()?.setIsRN()
    }

    /**
     * Returns a guaranteed map of options (environment and isAdvancedDevice) that we can use
     * to configure NeuroID properly
     */
    @VisibleForTesting
    internal fun parseOptions(rnOptions: ReadableMap?): Map<RNConfigOptions, Any> {
        // defaults
        var isAdvancedDevice = false
        var environment = NeuroID.PRODUCTION

        val options = mutableMapOf<RNConfigOptions, Any>()
        rnOptions?.let {rnOptionsMap ->
            // set the is advanced flag true or false from isAdvancedDevice option, default false
            if (rnOptionsMap.hasKey(RNConfigOptions.isAdvancedDevice.name)) {
                rnOptionsMap.getBoolean(RNConfigOptions.isAdvancedDevice.name).let {
                    isAdvancedDevice = it
                }
            }
            // set the environment params from the environment option, default PRODUCTION
            if (rnOptionsMap.hasKey(RNConfigOptions.environment.name)) {
                rnOptionsMap.getString(RNConfigOptions.environment.name)?.let {
                    when (it) {
                        NeuroID.PRODSCRIPT_DEVCOLLECTION -> environment =
                            NeuroID.PRODSCRIPT_DEVCOLLECTION

                        NeuroID.DEVELOPMENT -> NeuroID.DEVELOPMENT
                        else -> environment = NeuroID.PRODUCTION
                    }
                }
            }
        }
        options[RNConfigOptions.environment] = environment
        options[RNConfigOptions.isAdvancedDevice] = isAdvancedDevice
        return options
    }
}

enum class RNConfigOptions {
    isAdvancedDevice,
    environment
}