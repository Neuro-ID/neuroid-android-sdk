package com.neuroid.tracker.service

import android.content.Context
import android.util.Log
import com.neuroid.tracker.extensions.encodeToBase64
import com.neuroid.tracker.storage.getDataStoreInstance

object NIDServiceTracker {

    fun sendEventToServer(key: String, context: Context): Boolean {
        val listEvents = getDataStoreInstance(context).getAllEvents()

        if (listEvents.isEmpty().not()) {
            val listJson = "[${listEvents.joinToString(",")}]"
            Log.d("NeuroId", "Events: $listJson")
            Log.d("NeuroId", "Events Base64: ${listJson.encodeToBase64()}")
        }

        return true
    }
}