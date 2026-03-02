package com.neuroid.tracker.models

data class NIDConfiguration(val clientKey: String,
                            val isAdvancedDevice: Boolean,
                            val advancedDeviceKey: String? = null,
                            val useAdvancedDeviceProxy: Boolean = true,
                            val serverEnvironment: String = "production")