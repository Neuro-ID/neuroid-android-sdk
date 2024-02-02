package com.neuroid.tracker.storage

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.extensions.captureIntegrationHealthEvent
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTimerActive
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
    NIDDataStoreManagerInMemory.init(context)
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NIDDataStoreManagerInMemory
}

private object NIDDataStoreManagerInMemory: NIDDataStoreManager {
    var activityManager: ActivityManager? = null
    var bufferSize = 0

    fun init(context: Context) {
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR
    )
    private val listIdsExcluded = arrayListOf<String>()

    private var eventsList = mutableListOf<NIDEventModel>()

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        if (eventsList.isNotEmpty() &&
            (eventsList.last().type == "LOW_MEMORY" || eventsList.last().type == "queue full")) {
            return
        }
        val memInfo = getMemInfo()
        if (memInfo.lowMemory) {
            val metadataObj = JSONObject()
            metadataObj.put("isLowMemory", memInfo.lowMemory)
            metadataObj.put("total", memInfo.totalMem)
            metadataObj.put("available", memInfo.availMem)
            metadataObj.put("threshold", memInfo.threshold)
            val attrJSON = JSONArray().put(metadataObj)
            eventsList.add(generateEvent(LOW_MEMORY, attrJSON))

            return
        }
        if (eventsList.size > 2000) {
            val metadataObj = JSONObject()
            metadataObj.put("isLowMemory", memInfo.lowMemory)
            metadataObj.put("total", memInfo.totalMem)
            metadataObj.put("available", memInfo.availMem)
            metadataObj.put("threshold", memInfo.threshold)
            val attrJSON = JSONArray().put(metadataObj)
            eventsList.add(generateEvent(FULL_BUFFER, attrJSON))
            return
        }
        println("kurt_test: events count: ${eventsList.size}")
        bufferSize += event.getOwnJson().length
        println("kurt_test bufferSize: $bufferSize")
        println("-----------------")
        if (!listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
            // this event has been excluded and should not be saved
            return
        }

        event.gyro?.let {
            it.x = NIDSensorHelper.getGyroscopeInfo().x
            it.y = NIDSensorHelper.getGyroscopeInfo().y
            it.z = NIDSensorHelper.getGyroscopeInfo().z
        }

        event.accel?.let {
            it.x = NIDSensorHelper.getAccelerometerInfo().x
            it.y = NIDSensorHelper.getAccelerometerInfo().y
            it.z = NIDSensorHelper.getAccelerometerInfo().z
        }

        eventsList.add(event)

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
            "EVENT: ${event.type} - ${event.tgs} - $contextString"
        )

        if (NIDJobServiceManager.userActive.not()) {
            NIDJobServiceManager.userActive = true
            NIDJobServiceManager.restart()
        }

        if (listNonActiveEvents.contains(event.type)) {
            NIDTimerActive.restartTimerActive()
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

        NeuroID.getInstance()?.captureIntegrationHealthEvent(event = event)
    }

    fun generateEvent(type: String, attrJSON: JSONArray ) =
        NIDEventModel(
            type = type,
            ts = System.currentTimeMillis(),
            gyro = NIDSensorHelper.getGyroscopeInfo(),
            accel = NIDSensorHelper.getAccelerometerInfo(),
            attrs = attrJSON
        )

    @Synchronized
    override fun getAllEvents(): Set<String> {
        val previousEventsList = eventsList
        clearEvents()
        return toJsonList(previousEventsList).toSet()
    }

    @Synchronized
    override fun getAllEventsList(): List<String> {
        val previousEventsList = eventsList
        clearEvents()
        return toJsonList(previousEventsList)
    }

    @Synchronized
    override fun clearEvents() {
        eventsList = mutableListOf<NIDEventModel>()
        bufferSize = 0
    }

    @Synchronized
    override fun resetJsonPayload() {
        clearEvents()
    }

    private fun getMemInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        // check the memory check cost
        activityManager?.getMemoryInfo(memoryInfo)
        println("kurt_test memory: ${memoryInfo.threshold/1000000} ${memoryInfo.lowMemory} ${memoryInfo.availMem/1000000} ${memoryInfo.totalMem/1000000}")
        return memoryInfo
    }

    private fun toJsonList(list: MutableList<NIDEventModel>): List<String> {
        try {
            val jsonList = mutableListOf<String>()
            while (!list.isEmpty()) {
                jsonList.add(list.removeAt(0).getOwnJson())
            }
            return jsonList
        } catch (exception: OutOfMemoryError) {
            list.clear()
        }
        return mutableListOf()
    }

    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    override fun getJsonPayload(context: Context): String {
        TODO("Not yet implemented")
    }
}