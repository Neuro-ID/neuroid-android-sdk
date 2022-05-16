package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.BuildConfig
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.extensions.encodeToBase64
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NIDServiceTracker {
    @get:Synchronized @set:Synchronized
    var screenName = ""

    @get:Synchronized @set:Synchronized
    var screenActivityName = ""

    @get:Synchronized @set:Synchronized
    var screenFragName = ""

    fun sendEventToServer(key: String, context: Application): Pair<Int, Boolean> {
        val listEvents = getDataStoreInstance().getAllEvents()

        if (listEvents.isEmpty().not()) {
            val strUrl = if (BuildConfig.DEBUG) {
                "https://api.usw2-dev1.nidops.net/v3/c"
            } else {
                "https://api.neuro-id.com/v3/c"
            }

            NIDLog.d("NeuroID", "Url: $strUrl")
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
                .replace("\"url\":\"\"","\"url\":\"$screenActivityName\"")
                .replace("\\/","/")

            val data = getContentForm(context, listJson.encodeToBase64(), key)
            val stopLoopService = listEvents.last().contains(USER_INACTIVE)

            try {
                val os: OutputStream = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.flush()
                writer.close()
                os.close()

                NIDLog.d("NeuroID", "Events: $listJson")
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

    private fun getContentForm(context: Application, events: String, key: String): String {
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
            "url" to screenActivityName,
            "jsv" to "4.android-1.2.1",
            "events" to events
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