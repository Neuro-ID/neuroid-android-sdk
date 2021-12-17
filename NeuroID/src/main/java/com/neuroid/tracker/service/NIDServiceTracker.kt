package com.neuroid.tracker.service

import android.content.Context
import android.util.Log
import com.neuroid.tracker.BuildConfig
import com.neuroid.tracker.extensions.encodeToBase64
import com.neuroid.tracker.storage.getDataStoreInstance
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NIDServiceTracker {

    fun sendEventToServer(key: String, context: Context): Int {
        val listEvents = getDataStoreInstance().getAllEvents()

        if (listEvents.isEmpty().not()) {
            val strUrl = if (BuildConfig.DEBUG) {
                "https://api.usw2-dev1.nidops.net/v3/c"
            } else {
                "https://api.usw2-dev1.nidops.net/v3/c"
            }

            val url = URL(strUrl)
            val conn:HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.readTimeout = 5000
            conn.connectTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            conn.setRequestProperty("Authorization", "Basic $key")

            val listJson = "[${listEvents.joinToString(",")}]"
            val data = getContentForm(context, listJson.encodeToBase64(), key)

            try {
                val os: OutputStream = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.flush()
                writer.close()
                os.close()

                Log.d("NeuroId", "Events: $listJson")
                val code = conn.responseCode
                val message = conn.responseMessage

                return if (code in 200..299) {
                    NID_OK_SERVICE
                } else {
                    Log.d("NeuroId", "Error service: $message")
                    NID_ERROR_SERVICE
                }
            } catch (ex: Exception) {
                Log.d("NeuroId", "An error has occurred: ${ex.message}")
                return NID_ERROR_SYSTEM
            } finally {
                conn.disconnect()
            }
        } else {
            return NID_NO_EVENTS_ALLOWED
        }
    }

    private fun getContentForm(context: Context, events: String, key: String): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)
        val hashMapParams = hashMapOf(
            "key" to key,
            "id" to sharedDefaults.createRequestId(),
            "siteId" to "undefined",
            "sid" to sharedDefaults.getSessionID(),
            "cid" to sharedDefaults.getClientId(),
            "aid" to "null",
            "did" to sharedDefaults.getDeviceId(),
            "uid" to sharedDefaults.getUserId(),
            "pid" to sharedDefaults.getPageId(),
            "iid" to sharedDefaults.getIntermediateId(),
            "url" to "null",
            "jsv" to sharedDefaults.getSDKVersion(),
            "events" to events
        )

        Log.d("NeuroId", "---- Params Form ----")
        val dataForm = hashMapParams.map {
            Log.d("NeuroId", "${it.key}: ${it.value}")
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }.joinToString("&")

        return dataForm
    }

    const val NID_ERROR_SERVICE = 0x01
    const val NID_ERROR_SYSTEM = 0x02
    const val NID_NO_EVENTS_ALLOWED = 0x03
    const val NID_OK_SERVICE = 0x04
}