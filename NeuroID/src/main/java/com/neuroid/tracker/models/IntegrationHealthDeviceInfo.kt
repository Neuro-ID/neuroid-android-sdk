package com.neuroid.tracker.models

data class IntegrationHealthDeviceInfo(
    val name: String,
    val systemName: String,
    val systemVersion: String,
    val isSimulator: Boolean,
    val Orientation: DeviceOrientation,
    val model: String,
    val type: String,
    val customDeviceType: String,
    val nidSDKVersion: String,
)

data class DeviceOrientation(
    val rawValue: Int,
    var isFlat: Boolean,
    var isPortrait: Boolean,
    var isLandscape: Boolean,
    var isValid: Boolean,
)
