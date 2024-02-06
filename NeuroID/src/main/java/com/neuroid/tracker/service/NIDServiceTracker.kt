package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.LOW_MEMORY
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.models.NIDEventModel
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

    // a static payload to send if OOM occurs
    private var oomPayload = ""

    suspend fun sendEventToServer(
        key: String,
        endpoint: String,
        context: Application,
        events: Set<String>? = null
    ): Pair<Int, Boolean> {

        initializeStaticPayload(context)

        var data = ""
        var stopLoopService = false;

        try {
            val listEvents = getEvents(context, events)

            if (listEvents.isEmpty()) {
                // nothing to send
                return Pair(NID_ERROR_SERVICE, false)
            }

            stopLoopService = listEvents.last().get("type") == USER_INACTIVE

            data = getContentJson(context, JSONArray(listEvents))
                .replace("\\/", "/")

            NIDLog.d("NeuroID", "payload: ${listEvents.size} events; ${data.length} bytes")

            NeuroID.getInstance()?.saveIntegrationHealthEvents()
        } catch (exception: OutOfMemoryError) {
            // make a best effort attempt to continue and send an out of memory event
            data = oomPayload
        }

        // Allow for override of this URL in config
        NIDLog.d("NeuroID", "Url: $endpoint")
        val url = URL("${endpoint}/${key}")

        for (retryAttempts in 0 until MAX_RETRY_ATTEMPTS) {
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doInput = true
                conn.doOutput = true
                conn.readTimeout = 5000
                conn.connectTimeout = 5000
                conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                val os: OutputStream = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.flush()
                writer.close()
                os.close()

                val code = conn.responseCode
                val message = conn.responseMessage

                if (code == 200) {
                    NIDLog.d("NeuroID", "Http response code (${retryAttempts}): $code")
                    return Pair(code, stopLoopService)
                } else {
                    NIDLog.e("NeuroID", "Error service (${retryAttempts}): $message Code:$code")
                    // Retry in case of HTTP error
                }
            } catch (ex: Exception) {
                NIDLog.e("NeuroID", "An error has occurred (${retryAttempts}): ${ex.message}")
                // Retry in case of HTTP error
            } finally {
                conn.disconnect()
            }
        }

        // If we reach here, it means we've exhausted all retries and still encountered an error.
        // Return the error with the last HTTP response code received.
        return Pair(NID_ERROR_SERVICE, stopLoopService)
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

    /**
     * Create a payload that can be used in the event of an OOM error without additional allocations.
     */
    private suspend fun initializeStaticPayload(context: Context) {
        if (oomPayload.isEmpty()) {
            oomPayload = getContentJson(
                context,
                JSONArray(
                    listOf(
                        NIDEventModel(
                            type = "OUT_OF_MEMORY",
                            ts = System.currentTimeMillis()
                        ).getJSONObject()
                    )
                )
            )
        }
    }

    /**
     * Get the current system memory state.
     */
    private fun getMemInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    /**
     * Get the events that should be sent.  The 3 scenarios are:
     * 1. if provided, the set of string encoded events should be used and converted to the correct
     * structure
     * 2. if system memory is low, create a low memory event and return that
     * 3. get the current list of events from the data store manager
     */
    private fun getEvents(context: Context, events: Set<String>?): List<JSONObject> {
        if (events != null) {
            // convert the provided events set to a sorted list
            return events.map{JSONObject(it)}.sortedBy{it.getLong("ts")}
        }

        val memInfo = getMemInfo(context)
        if (memInfo.lowMemory) {
            val event = NIDEventModel(
                type = LOW_MEMORY,
                ts = System.currentTimeMillis(),
                attrs = JSONArray().put(JSONObject().apply {
                    put("isLowMemory", memInfo.lowMemory)
                    put("total", memInfo.totalMem)
                    put("available", memInfo.availMem)
                    put("threshold", memInfo.threshold)
                })
            )

            // add the low memory event to stop collecting additional events
            getDataStoreInstance().saveEvent(event)

            // return the low memory event to be sent
            return listOf(event.getJSONObject())
        } else {
            // return the current event collection
            return getDataStoreInstance().getAllEventsList()
        }
    }

    const val NID_OK_SERVICE = 0x01
    const val NID_ERROR_SERVICE = 0x02
}