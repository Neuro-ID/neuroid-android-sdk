package com.neuroid.tracker.models

import org.json.JSONObject

data class NIDLocation(
    var latitude: Double,
    var longitude: Double,
    var authorizationStatus: String,
) {
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("latitude", latitude)
        jsonObject.put("longitude", longitude)
        jsonObject.put("authorizationStatus", authorizationStatus)
        return jsonObject
    }
}
