package com.neuroid.tracker.models

data class NIDConfiguration(val clientKey: String,
                            val isAdvancedDevice: Boolean,
                            val advancedDeviceKey: String? = null,
                            val useAdvancedDeviceProxy: Boolean = false,
                            val hostReactNativeVersion: String = "",
                            val serverEnvironment: String = "production")