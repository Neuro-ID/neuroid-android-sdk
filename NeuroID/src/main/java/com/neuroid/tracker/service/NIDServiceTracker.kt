package com.neuroid.tracker.service

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.OUT_OF_MEMORY
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDVersion

object NIDServiceTracker {

    // a static payload to send if OOM occurs
    private var oomPayload = ""

    /**
     * Create a payload that can be used in the event of an OOM error without additional allocations.
     */
    private suspend fun initializeStaticPayload(context: Context) {
        if (oomPayload.isEmpty()) {
            oomPayload = getContentJson(
                context,
                listOf(
                    NIDEventModel(
                        type = OUT_OF_MEMORY,
                        ts = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    suspend fun sendEventToServer(
        eventSender: NIDEventSender,
        key: String,
        context: Application,
        events: List<NIDEventModel>,
        eventReportCallback: NIDResponseCallBack,
    ) {
        initializeStaticPayload(context)

        var data = ""
        try {
            if(events.isEmpty()) {
                // nothing to send
                return
            }


            data = getContentJson(context, events)

            NIDLog.d("NeuroID", "payload: ${events.size} events; ${data.length} bytes")
            NeuroID.getInstance()?.saveIntegrationHealthEvents()
        } catch (exception: OutOfMemoryError) {
            // make a best effort attempt to continue and send an out of memory event
            data = oomPayload
        }

        eventSender.sendTrackerData(data, key, eventReportCallback)
    }

     fun getContentJson(
        context: Context,
        events: List<NIDEventModel>
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

        val jsonBody = mapOf(
            "siteId" to siteId,
            "userId" to userID,
            "clientId" to sharedDefaults.getClientId(),
            "identityId" to userID,
            "registeredUserId" to registeredUserID,
            "pageTag" to screenActivityName,
            "pageId" to rndmId,
            "tabId" to rndmId,
            "responseId" to sharedDefaults.generateUniqueHexId(),
            "url" to "$ANDROID_URI${screenActivityName}",
            "jsVersion" to "5.0.0",
            "sdkVersion" to NIDVersion.getSDKVersion(),
            "environment" to environment,
            "jsonEvents" to events
        )

        // using this JSON library (already included) does not escape /
        val gson: Gson = GsonBuilder().create()
        return gson.toJson(jsonBody)
    }
}