package com.neuroid.tracker.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.BATTERY_SERVICE
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import org.json.JSONObject


class NIDMetaData(context: Context) {
    private val brand = Build.BRAND
    private var device = Build.DEVICE
    private var display = Build.DISPLAY
    private var manufacturer = Build.MANUFACTURER
    private var model = Build.MODEL
    private var product = Build.PRODUCT
    private var osVersion = "${Build.VERSION.SDK_INT ?: ""}"
    private var displayResolution = ""
    private var carrier = ""
    private var totalMemory: Double = (-1).toDouble()
    private var batteryLevel = -1
    private val isJailBreak: Boolean
    private var isWifiOn: Boolean?
    private val isSimulator: Boolean


    init {
        displayResolution = getScreenResolution(context)
        carrier = getCarrierName(context)
        totalMemory = getMemory(context)
        batteryLevel = getBatteryLevel(context)
        isJailBreak = RootHelper().isRooted(context)
        isWifiOn = getWifiStatus(context)
        isSimulator = RootHelper().isProbablyEmulator()
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

    private fun getWifiStatus(context: Context): Boolean? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled
        } catch (ex: Exception) {
            //No Wifi Permissions
            null
        }
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
        jsonObject.put("isJailBreak", isJailBreak)
        jsonObject.put("isWifiOn", isWifiOn)
        jsonObject.put("isSimulator", isSimulator)
        return jsonObject
    }

    override fun toString(): String = toJson().toString()


}