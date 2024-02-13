package com.neuroid.tracker.service

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.storage.NIDDataStoreManager
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
        eventReportCallback: NIDResponseCallBack,
        dataStoreManager: NIDDataStoreManager = getDataStoreInstance()
    ) {
        val listEvents = (events ?: dataStoreManager.getAllEvents()).sortedBy {
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
            NIDLog.d(msg="payload Json::: $data")

            eventSender.sendTrackerData(data, key, eventReportCallback)
        }
    }

    @VisibleForTesting
    suspend fun getContentJson(
        context: Context,
        events: JSONArray
    ): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)

        val userID:String? = if(NeuroID.getInstance()?.getUserID() != null) {
            NeuroID.getInstance()?.getUserID()
        } else {
            null
        }
        val registeredUserID:String? = if(NeuroID.getInstance()?.getRegisteredUserID() != null) {
            NeuroID.getInstance()?.getRegisteredUserID()
        } else {
            null
        }

        val jsonBody = JSONObject().apply {
            put("siteId", siteId)
            put("userId", userID)
            put("clientId", sharedDefaults.getClientId())
            put("identityId", userID)
            put("registeredUserId", registeredUserID)
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