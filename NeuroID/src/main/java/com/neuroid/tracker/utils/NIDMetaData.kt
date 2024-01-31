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
import android.location.LocationListener
import android.location.LocationManager

import androidx.core.app.ActivityCompat

class NIDMetaData(context: Context) {
    private var locationListener: LocationListener? = null
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
    private val gpsCoordinates: NIDLocation = NIDLocation(-1.0, -1.0, "none")

    init {
        displayResolution = getScreenResolution(context)
        carrier = getCarrierName(context)
        totalMemory = getMemory(context)
        batteryLevel = getBatteryLevel(context)
        isJailBreak = RootHelper().isRooted(context)
        isWifiOn = getWifiStatus(context)
        isSimulator = RootHelper().isProbablyEmulator()
        getLocation(context)
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

    internal fun getLocation(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // need to request the permissions, return
            return
        }

        // setup location manager
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // get last position if available, take highest accuracy from available providers
        val providers = locationManager.getProviders(true)
        var smallestAccuracyMeters = Float.MAX_VALUE
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                if (location.accuracy < smallestAccuracyMeters) {
                    gpsCoordinates.longitude = location.longitude
                    gpsCoordinates.latitude = location.latitude
                    gpsCoordinates.authorizationStatus = "authorizedAlways"
                    smallestAccuracyMeters = location.accuracy
                }
            }
        }

        // unregister existing location listener, just in case
        locationListener?.let {
            locationManager.removeUpdates(it)
        }

        // setup new location listener, just in case
        locationListener = LocationListener { location ->
            gpsCoordinates.longitude = location.latitude
            gpsCoordinates.latitude = location.longitude
            gpsCoordinates.authorizationStatus = "authorizedAlways"
        }

        // register new listener, 1 minute min time interval and 10 meter min distance interval
        // use GPS, highest accuracy
        locationListener?.let {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                60000L, 10F, it)
        }
        NIDLog.d(tag = "TESTING", msg = "post check - ${this.gpsCoordinates}")
    }

    internal fun unregisterLocationListener(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener?.let {
            locationManager.removeUpdates(it)
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
        jsonObject.put("gpsCoordinates", gpsCoordinates.toJson())
        return jsonObject
    }

    override fun toString(): String = toJson().toString()


}