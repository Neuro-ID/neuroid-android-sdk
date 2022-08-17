package com.neuroid.tracker.service

import android.app.Application
import android.content.Context
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDVersion
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NIDServiceTracker {
    @get:Synchronized
    @set:Synchronized
    var screenName = ""

    @get:Synchronized
    @set:Synchronized
    var screenActivityName = ""

    @get:Synchronized
    @set:Synchronized
    var screenFragName = ""

    var environment = ""
    var siteId = ""
    var rndmId = ""
    var firstScreenName = ""

    suspend fun sendEventToServer(
        key: String,
        endpoint: String,
        context: Application
    ): Pair<Int, Boolean> {
        val listEvents = getDataStoreInstance().getAllEvents().sortedBy {
            val event = JSONObject(it)
            event.getLong("ts")
        }
        if (listEvents.isEmpty().not()) {
            // Allow for override of this URL in config

            NIDLog.d("NeuroID", "Url: $endpoint")
            val url = URL(endpoint)
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.readTimeout = 5000
            conn.connectTimeout = 5000
            conn.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            conn.setRequestProperty("site_key", key)

            val listJson = listEvents.map {
                if (it.contains("\"CREATE_SESSION\"")) {
                    JSONObject(it.replace("\"url\":\"\"", "\"url\":\"$ANDROID_URI$firstScreenName\""))
                } else {
                    JSONObject(it)
                }
            }

            val jsonListEvents = JSONArray(listJson)

            val data = getContentJson(context, jsonListEvents)
                .replace("\\/", "/")
            val stopLoopService = listEvents.last().contains(USER_INACTIVE)
            NIDLog.d("NeuroID", "payload Json:: $data")

            try {
                val os: OutputStream = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.flush()
                writer.close()
                os.close()

                val code = conn.responseCode
                val message = conn.responseMessage

                return if (code in 200..299) {
                    NIDLog.d("NeuroID", "Http response code: $code")
                    Pair(NID_OK_SERVICE, stopLoopService)
                } else {
                    NIDLog.e("NeuroID", "Error service: $message")
                    Pair(NID_ERROR_SERVICE, stopLoopService)
                }
            } catch (ex: Exception) {
                NIDLog.e("NeuroID", "An error has occurred: ${ex.message}")
                return Pair(NID_ERROR_SERVICE, stopLoopService)
            } finally {
                conn.disconnect()
            }
        } else {
            return Pair(NID_ERROR_SERVICE, false)
        }
    }

    suspend fun getContentJson(
        context: Context,
        events: JSONArray
    ): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)
        rndmId = rndmId.ifBlank { sharedDefaults.getHexRandomID() }

        val jsonBody = JSONObject().apply {
            put("siteId", siteId)
            put("userId", sharedDefaults.getSessionID())
            put("clientId", sharedDefaults.getClientId())
            put("identityId", sharedDefaults.getUserId())
            put("pageTag", screenActivityName)
            put("pageId", rndmId)
            put("tabId", rndmId)
            put("responseId", sharedDefaults.generateUniqueHexId())
            put("url", "$ANDROID_URI$screenActivityName")
            put("jsVersion", NIDVersion.getSDKVersion())
            put("environment", environment)
            put("jsonEvents", events)
        }

        return jsonBody.toString()
    }

    const val NID_OK_SERVICE = 0x01
    const val NID_ERROR_SERVICE = 0x02
}