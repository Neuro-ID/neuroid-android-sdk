package com.neuroid.tracker.extensions

import android.app.Application
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.neuroid.tracker.NeuroID

class NIDRNBuilder(val application: Application? = null,
                   val clientKey: String = "",
                   val rnOptions: ReadableMap? = null) {

    fun build() {
        var isAdvancedDevice = false
        var environment = NeuroID.PRODUCTION

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
                        NeuroID.PRODUCTION -> environment = NeuroID.PRODUCTION
                        NeuroID.PRODSCRIPT_DEVCOLLECTION -> environment =
                            NeuroID.PRODSCRIPT_DEVCOLLECTION

                        NeuroID.DEVELOPMENT -> NeuroID.DEVELOPMENT
                        else -> environment = NeuroID.PRODUCTION
                    }
                }
            }
        }

        // call NeuroID builder to initialize NeuroID with the options.
        Log.d("NIDRNBuilder", "NIDRNBuilder isAdvancedDevice $isAdvancedDevice environment: $environment")
        NeuroID.Builder(application, clientKey, isAdvancedDevice, environment).build()
    }
}

enum class RNConfigOptions {
    isAdvancedDevice,
    environment
}