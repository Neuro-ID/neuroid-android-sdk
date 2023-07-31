package com.neuroid.tracker.service

import android.app.Application
import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
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

    var registeredViews: MutableSet<String> = mutableSetOf()

    var environment = ""
    var siteId = ""
    var rndmId = ""
    var firstScreenName = ""
    const val MAX_RETRY_ATTEMPTS = 3


    suspend fun sendEventToServer(
        key: String,
        endpoint: String,
        context: Application,
        events: Set<String>? = null
    ): Pair<Int, Boolean> {
        val listEvents = (events ?: getDataStoreInstance().getAllEvents()).sortedBy {
            val event = JSONObject(it)
            event.getLong("ts")
        }

        if (listEvents.isEmpty().not()) {
            NeuroID.getInstance()?.saveIntegrationHealthEvents()
            // Allow for override of this URL in config

            NIDLog.d("NeuroID", "Url: $endpoint")
            val url = URL("${endpoint}/${key}")
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

            val listJson = listEvents.map {
                if (it.contains("\"CREATE_SESSION\"")) {
                    JSONObject(
                        it.replace(
                            "\"url\":\"\"",
                            "\"url\":\"$ANDROID_URI$firstScreenName\""
                        )
                    )
                } else {
                    JSONObject(it)
                }
            }

            val jsonListEvents = JSONArray(listJson)

            val data = getContentJson(context, jsonListEvents)
                .replace("\\/", "/")
            val stopLoopService = listEvents.last().contains(USER_INACTIVE)
            NIDLog.d("NeuroID", "payload Json::: $data")

            var retryAttempts = 0
            while (retryAttempts < MAX_RETRY_ATTEMPTS) {
                try {
                    val os: OutputStream = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(data)
                    writer.flush()
                    writer.close()
                    os.close()

                    val code = conn.responseCode
                    val message = conn.responseMessage

                    if (code == 200) {
                        NIDLog.d("NeuroID", "Http response code: $code")
                        return Pair(code, stopLoopService)
                    } else {
                        NIDLog.e("NeuroID", "Error service: $message Code:$code")
                        // Retry in case of HTTP error
                        retryAttempts++
                    }
                } catch (ex: Exception) {
                    NIDLog.e("NeuroID", "An error has occurred: ${ex.message}")
                    // Retry in case of HTTP error
                    retryAttempts++
                } finally {
                    conn.disconnect()
                }
            }

            // If we reach here, it means we've exhausted all retries and still encountered an error.
            // Return the error with the last HTTP response code received.
            return Pair(NID_ERROR_SERVICE, stopLoopService)
        } else {
            return Pair(NID_ERROR_SERVICE, false)
        }
    }

    suspend fun getContentJson(
        context: Context,
        events: JSONArray
    ): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)

        val jsonBody = JSONObject().apply {
            put("siteId", siteId)
            put("userId", sharedDefaults.getUserId())
            put("clientId", sharedDefaults.getClientId())
            put("identityId", sharedDefaults.getUserId())
            put("pageTag", screenActivityName)
            put("pageId", rndmId)
            put("tabId", rndmId)
            put("responseId", sharedDefaults.generateUniqueHexId())
            put("url", "$ANDROID_URI$screenActivityName")
            put("jsVersion", "5.0.0")
            put("sdkVersion", NIDVersion.getSDKVersion())
            put("environment", environment)
            put("jsonEvents", events)
        }

        return jsonBody.toString()
    }

    const val NID_OK_SERVICE = 0x01
    const val NID_ERROR_SERVICE = 0x02
}