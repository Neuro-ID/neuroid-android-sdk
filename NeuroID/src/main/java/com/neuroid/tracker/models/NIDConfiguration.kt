package com.neuroid.tracker.models

data class NIDConfiguration(
    val clientKey: String,
    val region: NIDRegion = NIDRegion.US_WEST_DEFAULT,
    val isAdvancedDevice: Boolean = false,
    val advancedDeviceKey: String? = null,
    val useAdvancedDeviceProxy: Boolean = true,
)
