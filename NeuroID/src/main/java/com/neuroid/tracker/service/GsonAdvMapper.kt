package com.neuroid.tracker.service

import com.google.gson.Gson

class GsonAdvMapper {
    fun getKey(data: String): AdvancedDeviceKey =
        Gson().fromJson(data, AdvancedDeviceKey::class.java)
}