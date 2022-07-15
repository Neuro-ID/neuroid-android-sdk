package com.neuroid.tracker.storage

import com.neuroid.tracker.extensions.encodeToBase64
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class NIDPreferences(
    val sessionId: String,
    val clientId: String,
    val userId: String,
    val deviceId: String,
    val intermediateId: String,
    val events: Set<String>,
) {
    val pageId: String
        get() = run {
            val x = 1
            val now = System.currentTimeMillis()
            val rawId = (now - 1488084578518) * 1024 + (x + 1)
            String.format("%02x", rawId)
        }
    val locale = Locale.getDefault().toString()
    val language = Locale.getDefault().language
    val userAgent = System.getProperty("http.agent").orEmpty()
    val timeZone = 300
    val plataform = "Android"

    private var sequenceId = 1

    fun createRequestId(): String {
        val epoch = 1488084578518
        val now = System.currentTimeMillis()
        val rawId = (now - epoch) * 1024 + sequenceId
        sequenceId += 1

        return String.format("%02x", rawId)
    }

    fun getJsonEvents(): String {
        val array = JSONArray()
        events.forEach {
            array.put(JSONObject(it))
        }
        val result = array.toString()
        NIDLog.d("NeuroID", "Events: $result")
        return result.replace("\"url\":\"\"", "\"url\":\"${NIDServiceTracker.screenActivityName}\"")
            .replace("\\/", "/").encodeToBase64()
    }

}