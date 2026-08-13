package com.neuroid.tracker.utils

class NIDTagUtils {
    fun getBuildTags(): String? = android.os.Build.TAGS

    fun getFingerprint(): String = android.os.Build.FINGERPRINT

    fun getManufacturer(): String = android.os.Build.MANUFACTURER

    fun getBrand(): String = android.os.Build.BRAND

    fun getModel(): String = android.os.Build.MODEL

    fun getDevice(): String = android.os.Build.DEVICE

    fun getBoard(): String = android.os.Build.BOARD

    fun getHardware(): String = android.os.Build.HARDWARE

    fun getProduct(): String = android.os.Build.PRODUCT

    fun getHost(): String = android.os.Build.HOST
}
