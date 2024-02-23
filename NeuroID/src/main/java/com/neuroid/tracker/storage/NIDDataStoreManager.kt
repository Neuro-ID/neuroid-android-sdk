package com.neuroid.tracker.storage

import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.extensions.captureIntegrationHealthEvent
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTimerActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue

interface NIDDataStoreManager {
    fun saveEvent(tempEvent: NIDEventModel)
    fun getAllEvents(): List<NIDEventModel>
    fun addViewIdExclude(id: String)
    fun clearEvents()
    fun queueEvent(tempEvent: NIDEventModel)
    fun saveAndClearAllQueuedEvents()
}

@VisibleForTesting // Should we make the private?
fun NeuroID.getDataStoreInstance():NIDDataStoreManager {
    return dataStore
}

internal class NIDDataStoreManagerImp(private val ioDispatcher: CoroutineScope = CoroutineScope(Dispatchers.IO)) : NIDDataStoreManager {
    companion object {
        const val EVENT_BUFFER_MAX_COUNT = 1999
    }

    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    internal val listIdsExcluded = arrayListOf<String>()
    private var eventsList = mutableListOf<NIDEventModel>()
    internal var queuedEvents: Queue<NIDEventModel> = LinkedList()

    // Queue events that are set before sdk is started
    @Synchronized
    override fun queueEvent(tempEvent: NIDEventModel) {
        val event = tempEvent.copy(
            ts = System.currentTimeMillis(),
            gyro = NIDSensorHelper.getGyroscopeInfo(),
            accel = NIDSensorHelper.getAccelerometerInfo()
        )

        queuedEvents.add(event)
    }

    @Synchronized
    override fun saveAndClearAllQueuedEvents() {
        eventsList.addAll(queuedEvents)
        queuedEvents.clear()
    }

    @Synchronized
    override fun saveEvent(tempEvent: NIDEventModel) {
        // add TS, Gyro, and Accel to every event
        val event = tempEvent.copy(
            ts = System.currentTimeMillis(),
            gyro = NIDSensorHelper.getGyroscopeInfo(),
            accel = NIDSensorHelper.getAccelerometerInfo()
        )

        if (NeuroID.getInstance()?.nidJobServiceManager?.isStopped() == true) {
            return
        }

        if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
            val lastEventType = if (eventsList.isEmpty()) { "" } else { eventsList.last().type }
            if (NeuroID.getInstance()?.lowMemory == true || lastEventType == FULL_BUFFER) {
                // no new events should be captured, drop the new event
                NIDLog.w("NeuroID", "Data store buffer ${lastEventType}, ${event.type} dropped")
                return
            } else if (eventsList.size > EVENT_BUFFER_MAX_COUNT) {
                // add a full buffer event and drop the new event
                eventsList.add(NIDEventModel(type = FULL_BUFFER, ts = System.currentTimeMillis()))
                NIDLog.w("NeuroID", "Data store buffer ${FULL_BUFFER}, ${event.type} dropped")
                return
            }

            if (!listNonActiveEvents.contains(event.type)) {
                NIDTimerActive.restartTimerActive()
            }

            // capture the event
            eventsList.add(event)
            logEvent(event)
            NeuroID.getInstance()?.captureIntegrationHealthEvent(event = event)
        }

        when (event.type) {
            BLUR -> {
                ioDispatcher.launch {
                    NeuroID.getInstance()?.nidJobServiceManager?.sendEventsNow()
                }
            }
            CLOSE_SESSION -> {
                ioDispatcher.launch {
                    NeuroID.getInstance()?.nidJobServiceManager?.sendEventsNow(true)
                }
            }
        }
    }

    @Synchronized
    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    /*
        Event store is cleared out. Where necessary, events are updated to fill in missing values.
     */
    override fun getAllEvents(): List<NIDEventModel> {
        val previousEventsList = swapEvents()

        previousEventsList.forEachIndexed { index, item ->
            var updateEvent = false

            // if the accelerometer values are null, use the current values
            var accel = item.accel
            accel?.let {
                if (it.x == null && it.y == null && it.z == null) {
                    updateEvent = true
                    accel = NIDSensorHelper.getAccelerometerInfo()
                }
            }

            // if the gyro values are null, use the current values
            var gyro = item.gyro
            gyro?.let {
                if (it.x == null && it.y == null && it.z == null) {
                    updateEvent = true
                    gyro = NIDSensorHelper.getGyroscopeInfo()
                }
            }

            // update the url for create session events if not set
            var url = item.url
            if (item.type == CREATE_SESSION && url == "") {
                updateEvent = true
                url = "$ANDROID_URI${NeuroID.firstScreenName}"
            }

            if (updateEvent) {
                // update the event
                previousEventsList[index] = item.copy(
                    accel = accel,
                    gyro = gyro,
                    url = url
                )
            }
        }

        return previousEventsList
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

    private fun logEvent(event: NIDEventModel) {
        NIDLog.d(
            Constants.debugEventTag.displayName,
            ""
        ) {
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
                SET_VARIABLE -> contextString = event.v ?: ""
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

            "EVENT: ${event.type} - ${event.tgs} - ${contextString}"
        }
    }
}