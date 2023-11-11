package com.neuroid.tracker.service

import android.app.Application
import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDVersion
import org.json.JSONArray
import org.json.JSONObject

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

    suspend fun sendEventToServer(
        eventSender: NIDEventSender,
        key: String,
        context: Application,
        events: Set<String>? = null,
        eventReportCallback: NIDResponseCallBack
    ) {
        val listEvents = (events ?: getDataStoreInstance().getAllEvents()).sortedBy {
            val event = JSONObject(it)
            event.getLong("ts")
        }

        if (listEvents.isEmpty().not()) {
            NeuroID.getInstance()?.saveIntegrationHealthEvents()
            // Allow for override of this URL in config

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
            NIDLog.d("Neuro ID", "payload Json::: $data")

            eventSender.sendTrackerData(data, key, eventReportCallback)
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
}