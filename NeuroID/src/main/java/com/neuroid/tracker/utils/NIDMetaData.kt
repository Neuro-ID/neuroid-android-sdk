package com.neuroid.tracker.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.BATTERY_SERVICE
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import com.neuroid.tracker.models.NIDLocation
import org.json.JSONObject


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

import androidx.core.app.ActivityCompat


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
    private val gpsCoordinates: NIDLocation


    init {
        displayResolution = getScreenResolution(context)
        carrier = getCarrierName(context)
        totalMemory = getMemory(context)
        batteryLevel = getBatteryLevel(context)
        isJailBreak = RootHelper().isRooted(context)
        isWifiOn = getWifiStatus(context)
        isSimulator = RootHelper().isProbablyEmulator()

        gpsCoordinates = getCoordinates(context);
    }

    private fun getScreenResolution(context: Context): String = try {
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        "$width,$height"
    } catch (ex: Exception) {
        ""
    }

    private fun getCarrierName(context: Context): String = try {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.networkOperatorName
    } catch (ex: Exception) {
        ""
    }

    private fun getMemory(context: Context): Double = try {
        val actManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // Declaring MemoryInfo object
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
    } catch (ex: Exception) {
        0.toDouble()
    }

    private fun getBatteryLevel(context: Context): Int = try {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (ex: Exception) {
        0
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

    fun getCoordinates(context: Context): NIDLocation {
        var locationObj = NIDLocation(
            latitude = -1.0,
            longitude = -1.0,
            authorizationStatus = "unknown"
        )
        val location = getLocation(context)
        location?.let {
            val longitude = it.longitude
            val latitude = it.latitude
            println("Longitude: $longitude, Latitude: $latitude")

            locationObj = NIDLocation(
                it.longitude,
                it.latitude,
                "authorizedAlways"
            )
        }

        return locationObj
    }


    fun getLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permissions
            return null
        }

        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
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
        jsonObject.put("gpsCoordinates", gpsCoordinates.toJson())
        return jsonObject
    }

    override fun toString(): String = toJson().toString()


}