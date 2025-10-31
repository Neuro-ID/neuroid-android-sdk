package com.neuroid.tracker.utils

class NIDTagUtils {
    fun getBuildTags(): String? {
        return android.os.Build.TAGS
    }
    fun getFingerprint(): String {
        return android.os.Build.FINGERPRINT
    }
    fun getManufacturer(): String {
        return android.os.Build.MANUFACTURER
    }
    fun getBrand(): String {
        return android.os.Build.BRAND
    }
    fun getModel(): String {
        return android.os.Build.MODEL
    }
    fun getDevice(): String {
        return android.os.Build.DEVICE
    }
    fun getBoard(): String {
        return android.os.Build.BOARD
    }
    fun getHardware(): String {
        return android.os.Build.HARDWARE
    }
    fun getProduct(): String {
        return android.os.Build.PRODUCT
    }
    fun getHost(): String {
        return android.os.Build.HOST
    }
}