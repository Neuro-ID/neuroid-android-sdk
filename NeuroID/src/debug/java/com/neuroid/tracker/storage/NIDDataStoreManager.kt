package com.neuroid.tracker.storage

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.extensions.captureIntegrationHealthEvent
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

interface NIDDataStoreManager {
    fun saveEvent(event: NIDEventModel)
    suspend fun getAllEvents(): Set<String>
    fun addViewIdExclude(id: String)
    suspend fun clearEvents()
    fun resetJsonPayload()
    fun getJsonPayload(context: Context): String
}

fun initDataStoreCtx(context: Context) {
    NIDDataStoreManagerImp.init(context)
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NIDDataStoreManagerImp
}

private object NIDDataStoreManagerImp : NIDDataStoreManager {
    private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
    private const val NID_STRING_EVENTS = "NID_STRING_EVENTS"
    private const val NID_STRING_JSON_PAYLOAD = "NID_STRING_JSON_PAYLOAD"
    private var sharedPref: SharedPreferences? = null
    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    private val listIdsExcluded = arrayListOf<String>()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            sharedPref = context.getSharedPreferences(NID_SHARED_PREF_FILE, MODE_PRIVATE)
        }
    }

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        CoroutineScope(Dispatchers.IO).launch {
            if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
                val strEvent = event.getOwnJson()
                saveJsonPayload(strEvent, "\"${event.type}\"")

                var contextString: String? = ""
                when (event.type) {
                    SET_USER_ID -> contextString = "uid=${event.uid}"
                    CREATE_SESSION -> contextString =
                        "cid=${event.cid}, sh=${event.sh}, sw=${event.sw}"
                    APPLICATION_SUBMIT -> contextString = ""
                    TEXT_CHANGE -> contextString = "v=${event.v}, tg=${event.tg}"
                    "SET_CHECKPOINT" -> contextString = ""
                    "STATE_CHANGE" -> contextString = event.url ?: ""
                    KEY_UP -> contextString = "tg=${event.tg}"
                    KEY_DOWN -> contextString = "tg=${event.tg}"
                    INPUT -> contextString = "v=${event.v}, tg=${event.tg}"
                    FOCUS -> contextString = ""
                    BLUR -> contextString = ""
                    MOBILE_METADATA_ANDROID -> contextString = "meta=${event.metadata}"
                    "CLICK" -> contextString = ""
                    REGISTER_TARGET -> contextString =
                        "et=${event.et}, rts=${event.rts}, ec=${event.ec} v=${event.v} tg=${event.tg} meta=${event.metadata}"
                    "DEREGISTER_TARGET" -> contextString = ""
                    TOUCH_START -> contextString = "xy=${event.touches} tg=${event.tg}"
                    TOUCH_END -> contextString = "xy=${event.touches} tg=${event.tg}"
                    TOUCH_MOVE -> contextString = "xy=${event.touches} tg=${event.tg}"
                    CLOSE_SESSION -> contextString = ""
                    "SET_VARIABLE" -> contextString = event.v ?: ""
                    CUT -> contextString = ""
                    COPY -> contextString = ""
                    PASTE -> contextString = ""
                    WINDOW_RESIZE -> contextString = "h=${event.h}, w=${event.w}"
                    SELECT_CHANGE -> contextString = "tg=${event.tg}"
                    WINDOW_LOAD -> contextString = "meta=${event.metadata}"
                    WINDOW_UNLOAD -> contextString = "meta=${event.metadata}"
                    WINDOW_BLUR -> contextString = "meta=${event.metadata}"
                    WINDOW_FOCUS -> contextString = "meta=${event.metadata}"
                    CONTEXT_MENU -> contextString = "meta=${event.metadata}"
                    else -> {}
                }

                NIDLog.d(
                    Constants.debugEventTag.displayName,
                    "EVENT: ${event.type} - ${event.tgs} - ${contextString}"
                )

                if (NIDJobServiceManager.userActive.not()) {
                    NIDJobServiceManager.userActive = true
                    NIDJobServiceManager.restart()
                }

                if (!listNonActiveEvents.any { strEvent.contains(it) }) {
                    NIDTimerActive.restartTimerActive()
                }
                val lastEvents = getStringSet(NID_STRING_EVENTS)
                val newEvents = LinkedHashSet<String>()
                newEvents.addAll(lastEvents)
                newEvents.add(strEvent)
                putStringSet(NID_STRING_EVENTS, newEvents)

                NeuroID.getInstance()?.captureIntegrationHealthEvent(event = event)
            }

            when (event.type) {
                BLUR -> {
                    NIDJobServiceManager.sendEventsNow()
                }
                CLOSE_SESSION -> {
                    NIDJobServiceManager.sendEventsNow(true)
                }
            }
        }
    }

    override suspend fun getAllEvents(): Set<String> {
        val lastEvents = getStringSet(NID_STRING_EVENTS, emptySet())
        clearEvents()

        return lastEvents.map {
            it.replace(
                "\"gyro\":{\"x\":null,\"y\":null,\"z\":null}",
                "\"gyro\":{\"x\":${NIDSensorHelper.valuesGyro.axisX},\"y\":${NIDSensorHelper.valuesGyro.axisY},\"z\":${NIDSensorHelper.valuesGyro.axisZ}}"
            ).replace(
                "\"accel\":{\"x\":null,\"y\":null,\"z\":null}",
                "\"accel\":{\"x\":${NIDSensorHelper.valuesAccel.axisX},\"y\":${NIDSensorHelper.valuesAccel.axisY},\"z\":${NIDSensorHelper.valuesAccel.axisZ}}"
            )
        }.toSet()
    }

    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    override suspend fun clearEvents() {
        putStringSet(NID_STRING_EVENTS, emptySet())
    }

    override fun resetJsonPayload() {
        sharedPref?.let {
            with(it.edit()) {
                putStringSet(NID_STRING_JSON_PAYLOAD, emptySet())
                apply()
            }
        }
    }

    override fun getJsonPayload(context: Context): String {
        return createPayload(context)
    }

    private suspend fun putStringSet(key: String, stringSet: Set<String>) {
        sharedPref?.let {
            with(it.edit()) {
                putStringSet(key, stringSet)
                apply()
            }
        }
    }

    private suspend fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> {
        return sharedPref?.getStringSet(key, default) ?: default
    }

    private fun saveJsonPayload(event: String, eventType: String) {
        val jsonSet = sharedPref?.getStringSet(NID_STRING_JSON_PAYLOAD, emptySet()) ?: emptySet()
        var shouldAdd = true
        jsonSet.forEach {
            if (it.contains(eventType)) {
                shouldAdd = false
            }
        }

        if (shouldAdd) {
            val newEvents = LinkedHashSet<String>()
            newEvents.addAll(jsonSet)
            newEvents.add(event)

            sharedPref?.let {
                with(it.edit()) {
                    putStringSet(NID_STRING_JSON_PAYLOAD, newEvents)
                    apply()
                }
            }
        }
    }

    private fun createPayload(context: Context): String {
        val jsonSet = sharedPref?.getStringSet(NID_STRING_JSON_PAYLOAD, emptySet()) ?: emptySet()

        val listEvents = jsonSet.sortedBy {
            val event = JSONObject(it)
            event.getLong("ts")
        }

        if (listEvents.isEmpty()) {
            return ""
        } else {
            val listJson = listEvents.map {
                if (it.contains("\"CREATE_SESSION\"")) {
                    JSONObject(
                        it.replace(
                            "\"url\":\"\"",
                            "\"url\":\"$ANDROID_URI${NIDServiceTracker.firstScreenName}\""
                        )
                    )
                } else {
                    JSONObject(it)
                }
            }

            val jsonListEvents = JSONArray(listJson)

            return getContentJson(context, jsonListEvents)
                .replace("\\/", "/")
        }
    }

    private fun getContentJson(
        context: Context,
        events: JSONArray
    ): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)

        val jsonBody = JSONObject().apply {
            put("siteId", NIDServiceTracker.siteId)
            put("userId", sharedDefaults.getUserId())
            put("clientId", sharedDefaults.getClientId())
            put("identityId", sharedDefaults.getUserId())
            put("pageTag", NIDServiceTracker.screenActivityName)
            put("pageId", NIDServiceTracker.rndmId)
            put("tabId", NIDServiceTracker.rndmId)
            put("responseId", sharedDefaults.generateUniqueHexId())
            put("url", "$ANDROID_URI${NIDServiceTracker.screenActivityName}")
            put("jsVersion", "5.0.0")
            put("sdkVersion", NIDVersion.getSDKVersion())
            put("environment", NIDServiceTracker.environment)
            put("jsonEvents", events)
        }

        return jsonBody.toString()
    }
}