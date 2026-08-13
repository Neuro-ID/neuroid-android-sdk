package com.neuroid.tracker.models

data class NIDConfiguration(
    val clientKey: String,
    val isAdvancedDevice: Boolean,
    val advancedDeviceKey: String? = null,
    val useAdvancedDeviceProxy: Boolean = true,
    @Deprecated(
        "serverEnvironment is deprecated",
    )
    val serverEnvironment: String = "production",
    val region: NIDRegion = NIDRegion.usWest,
)
