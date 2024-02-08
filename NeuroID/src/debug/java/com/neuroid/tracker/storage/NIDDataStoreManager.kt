package com.neuroid.tracker.storage

import android.content.Context
import com.neuroid.tracker.BuildConfig
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
    fun getAllEventsList(): List<JSONObject>
    fun addViewIdExclude(id: String)
    fun clearEvents()
    fun resetJsonPayload()
    fun getJsonPayload(context: Context): String
}

fun initDataStoreCtx(context: Context) {
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NIDDataStoreManagerImp
}

private object NIDDataStoreManagerImp: NIDDataStoreManager {
    const val EVENT_BUFFER_MAX_COUNT = 1999

    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    private val listIdsExcluded = arrayListOf<String>()

    // buffer of captured events, all accesses must be synchronized
    private var eventsList = mutableListOf<NIDEventModel>()

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        if (NIDJobServiceManager.isStopped()) {
            return
        }

        if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
            val lastEventType = if (eventsList.isEmpty()) { "" } else { eventsList.last().type }

            if (lastEventType == LOW_MEMORY || lastEventType == FULL_BUFFER) {
                // no new events should be captured, drop the new event
                NIDLog.w("NeuroID", "Data store buffer ${lastEventType}, ${event.type} dropped")
            } else if (eventsList.size > EVENT_BUFFER_MAX_COUNT) {
                // add a full buffer event and drop the new event
                eventsList.add(NIDEventModel(type = FULL_BUFFER, ts = System.currentTimeMillis()))
                NIDLog.w("NeuroID", "Data store buffer ${FULL_BUFFER}, ${event.type} dropped")
            } else {
                // capture the event
                eventsList.add(event)
                logEvent(event)
            }

            if (NIDJobServiceManager.userActive.not()) {
                NIDJobServiceManager.userActive = true
                NIDJobServiceManager.restart()
            }

            if (!listNonActiveEvents.contains(event.type)) {
                NIDTimerActive.restartTimerActive()
            }
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

    override fun getAllEvents(): Set<String> {
        val previousEventsList = swapEvents()
        return toJsonList(previousEventsList).map{it.toString()}.toSet()
    }

    /**
     * Return the currently captured events, resetting the eventList to a new empty list.
     */
    override fun getAllEventsList(): List<JSONObject> {
        val previousEventsList = swapEvents()
        return toJsonList(previousEventsList)
    }

    override fun clearEvents() {
        swapEvents()
    }

    /**
     * Synchronize the fetching of the current event list and its replacement with a new event list.
     */
    @Synchronized
    private fun swapEvents(): MutableList<NIDEventModel> {
        val previousEventsList = eventsList
        eventsList = mutableListOf<NIDEventModel>()
        return previousEventsList
    }

    override fun resetJsonPayload() {
        swapEvents()
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

    /**
     * Convert a list of NIDEventModel to a list of JSONObject.  The provided list will be cleared
     * during the conversion.  Where necessary, events are updated to fill in missing values.
     */
    private fun toJsonList(list: MutableList<NIDEventModel>): List<JSONObject> {
        val jsonList = mutableListOf<JSONObject>()
        while (list.isNotEmpty()) {
            var event = list.removeAt(0)

            var updateEvent = false

            // if the accelerometer values are null, use the current values
            var accel = event.accel
            accel?.let {
                if (it.x == null && it.y == null && it.z == null) {
                    updateEvent = true
                    accel = NIDSensorHelper.getAccelerometerInfo()
                }
            }

            // if the gyro values are null, use the current values
            var gyro = event.gyro
            gyro?.let {
                if (it.x == null && it.y == null && it.z == null) {
                    updateEvent = true
                    gyro = NIDSensorHelper.getGyroscopeInfo()
                }
            }

            // update the url for create session events if not set
            var url = event.url
            if (event.type == CREATE_SESSION && url == "") {
                updateEvent = true
                url = "$ANDROID_URI${NIDServiceTracker.firstScreenName}"
            }

            if (updateEvent) {
                // update the event
                event = event.copy(
                    accel = accel,
                    gyro = gyro,
                    url = url
                )
            }

            jsonList.add(event.getJSONObject())
        }
        return jsonList
    }

    @Synchronized
    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    /**
     * Debug logging for captured events.
     */
    private fun logEvent(event: NIDEventModel) {
        NeuroID.getInstance()?.captureIntegrationHealthEvent(event = event)

        if (!BuildConfig.DEBUG) {
            return
        }

        // for debug
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
            "Event: ${event.type} - ${event.tgs} - $contextString"
        )
    }
}