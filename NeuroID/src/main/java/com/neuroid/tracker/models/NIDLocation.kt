package com.neuroid.tracker.models

import org.json.JSONObject

data class NIDLocation(
    val latitude: Double,
    val longitude: Double,
    val authorizationStatus: String
) {
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("latitude", latitude)
        jsonObject.put("longitude", longitude)
        jsonObject.put("authorizationStatus", authorizationStatus)
        return jsonObject
    }
}