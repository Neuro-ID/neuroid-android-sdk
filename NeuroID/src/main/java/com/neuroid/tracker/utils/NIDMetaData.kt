package com.neuroid.tracker.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import org.json.JSONObject


class NIDMetaData(context: Context) {
    val brand = Build.BRAND
    var device = Build.DEVICE
    var display = Build.DISPLAY
    var manufacturer = Build.MANUFACTURER
    var model = Build.MODEL
    var product = Build.PRODUCT
    var osVersion = "OS:${Build.VERSION.SDK_INT ?: ""}"
    var displayResolution = ""
    var carrier = ""
    var totalMemory: Double = (-1).toDouble()
    var batteryLevel = -1
    val isRooted: Boolean get() = RootHelper().isRooted()

    init {
        displayResolution = getScreenResolution(context)
        carrier = getCarrierName(context)
        totalMemory = getMemory(context)
        batteryLevel = getBatteryLevel(context)
    }

    private fun getScreenResolution(context: Context): String {
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        return "$width,$height"
    }

    private fun getCarrierName(context: Context): String {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.networkOperatorName
    }

    private fun getMemory(context: Context): Double {
        val actManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // Declaring MemoryInfo object
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
    }

    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("brand", brand)
        jsonObject.put("device", device)
        jsonObject.put("display", display)
        jsonObject.put("displayResolution", displayResolution)
        jsonObject.put("manufacturer", manufacturer)
        jsonObject.put("model", model)
        jsonObject.put("product", product)
        jsonObject.put("osVersion", osVersion)
        jsonObject.put("carrier", carrier)
        jsonObject.put("totalMemory", totalMemory)
        jsonObject.put("batteryLevel", batteryLevel)
        jsonObject.put("isRooted", isRooted)
        return jsonObject
    }

    override fun toString(): String = toJson().toString()


}