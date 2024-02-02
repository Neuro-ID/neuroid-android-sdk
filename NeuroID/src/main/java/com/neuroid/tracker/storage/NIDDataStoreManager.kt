package com.neuroid.tracker.storage

import android.content.Context
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
    fun getAllEvents(): Set<String>
    fun getAllEventsList(): List<String>
    fun addViewIdExclude(id: String)
    fun clearEvents()
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
    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    private val listIdsExcluded = arrayListOf<String>()
    private var eventsList = mutableListOf<NIDEventModel>()

    fun init(context: Context) {
    }

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {

            if (NIDJobServiceManager.userActive.not()) {
                NIDJobServiceManager.userActive = true
                NIDJobServiceManager.restart()
            }

            if (!listNonActiveEvents.contains(event.type)) {
                NIDTimerActive.restartTimerActive()
            }

            eventsList.add(event)

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
                ADVANCED_DEVICE_REQUEST -> contextString = "rid=${event.rid}, c=${event.c}"
                LOG -> contextString = "m=${event.m}, ts=${event.ts}, level=${event.level}"
                else -> {}
            }

            NIDLog.d(
                Constants.debugEventTag.displayName,
                "EVENT: ${event.type} - ${event.tgs} - ${contextString}"
            )

            NeuroID.getInstance()?.captureIntegrationHealthEvent(event = event)
        }

        when (event.type) {
            BLUR -> {
                CoroutineScope(Dispatchers.IO).launch {
                    NIDJobServiceManager.sendEventsNow()
                }
            }
            CLOSE_SESSION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    NIDJobServiceManager.sendEventsNow(true)
                }
            }
        }
    }

    @Synchronized
    override fun getAllEvents(): Set<String> {
        val lastEvents = eventsList
        clearEvents()

        return toJsonList(lastEvents).toSet()
    }

    @Synchronized
    override fun getAllEventsList(): List<String> {
        val lastEvents = eventsList
        clearEvents()

        return toJsonList(lastEvents)
    }

    @Synchronized
    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    @Synchronized
    override fun clearEvents() {
        eventsList = mutableListOf()
    }

    @Synchronized
    override fun resetJsonPayload() {
        clearEvents()
    }

    @Synchronized
    override fun getJsonPayload(context: Context): String {
        val listEvents = eventsList.map { e -> e.getOwnJson() }
        return createPayload(listEvents, context)
    }

    private fun createPayload(listEvents: List<String>, context: Context): String {
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

    private fun toJsonList(events: MutableList<NIDEventModel>) : List<String> {
        val jsonList = mutableListOf<String>()
        while (events.isNotEmpty()) {
            val event = events.removeAt(0)
            event.accel?.let {
                it.x = it.x ?: NIDSensorHelper.valuesAccel.axisX
                it.y = it.y ?: NIDSensorHelper.valuesAccel.axisY
                it.z = it.z ?:NIDSensorHelper.valuesAccel.axisZ
            }
            event.gyro?.let {
                it.x = it.x ?: NIDSensorHelper.valuesGyro.axisX
                it.y = it.y ?: NIDSensorHelper.valuesGyro.axisY
                it.z = it.z ?:NIDSensorHelper.valuesGyro.axisZ
            }
            jsonList.add(event.getOwnJson())
        }
        return jsonList
    }
}