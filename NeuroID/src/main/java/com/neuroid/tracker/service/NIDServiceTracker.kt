package com.neuroid.tracker.service

import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.storage.NIDPreferences
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDVersion
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

    fun sendEventToServer(
        key: String,
        endpoint: String,
        nidPreferences: NIDPreferences
    ): Pair<Int, Boolean> {
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
            "application/x-www-form-urlencoded;charset=UTF-8"
        )
        conn.setRequestProperty("Authorization", "Basic $key")

        val data = getContentForm(nidPreferences, key)
        val stopLoopService = nidPreferences.events.last().contains(USER_INACTIVE)

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
    }

    private fun getContentForm(nidPreferences: NIDPreferences, key: String): String {

        val hashMapParams = hashMapOf(
            "key" to key,
            "id" to nidPreferences.createRequestId(),
            "siteId" to "undefined",
            "sid" to nidPreferences.sessionId,
            "cid" to nidPreferences.clientId,
            "aid" to "null",
            "did" to nidPreferences.deviceId,
            "uid" to nidPreferences.userId,
            "pid" to nidPreferences.pageId,
            "iid" to nidPreferences.intermediateId,
            "url" to screenActivityName,
            "jsv" to NIDVersion.getSDKVersion(),
            "events" to nidPreferences.getJsonEvents()
        )

        NIDLog.d("NeuroId", "---- Params Form ----")
        val dataForm = hashMapParams.map {
            NIDLog.d("NeuroId", "${it.key}: ${it.value}")
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }.joinToString("&")

        return dataForm
    }

    const val NID_OK_SERVICE = 0x01
    const val NID_ERROR_SERVICE = 0x02
}