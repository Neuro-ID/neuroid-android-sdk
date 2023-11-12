package com.neuroid.tracker.utils

import com.google.gson.Gson
import com.neuroid.tracker.models.AdvancedDeviceKey

class GsonAdvMapper {
    fun getKey(data: String): AdvancedDeviceKey =
        Gson().fromJson(data, AdvancedDeviceKey::class.java)
}