package com.neuroid.tracker.storage

import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDRemoteConfigService
import com.neuroid.tracker.service.NIDSamplingService
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTimerActive
import java.util.LinkedList
import java.util.Queue

interface NIDDataStoreManager {
    fun saveEvent(event: NIDEventModel)

    fun getAllEvents(): List<NIDEventModel>

    fun clearEvents()

    fun queueEvent(tempEvent: NIDEventModel)

    fun saveAndClearAllQueuedEvents()

    fun isFullBuffer(): Boolean
}

@VisibleForTesting // Should we make the private?
fun NeuroIDPublic.getTestingDataStoreInstance(): NIDDataStoreManager? {
    return NeuroID.getInternalInstance()?.dataStore
}

internal class NIDDataStoreManagerImp(
    val logger: NIDLogWrapper,
    var samplingService: NIDSamplingService,
    var configService: NIDRemoteConfigService
) : NIDDataStoreManager {
    companion object {
        private val listNonActiveEvents =
            listOf(
                USER_INACTIVE,
                WINDOW_BLUR, // Block screen
            )
    }

    private var eventsList = mutableListOf<NIDEventModel>()
    internal var queuedEvents: Queue<NIDEventModel> = LinkedList()

    // Queue events that are set before sdk is started
    @Synchronized
    override fun queueEvent(tempEvent: NIDEventModel) {
        if (isFullBuffer()) {
            logger.w("NeuroID", "Data store buffer ${FULL_BUFFER}, ${tempEvent.type} dropped")
            return
        }
        if (!samplingService.isSessionFlowSampled()) {
            logger.i(msg="NID queueEvent() is not sampling ${tempEvent.type} siteID: ${NeuroID.linkedSiteID}")
            return
        }
        logger.i(msg="NID queueEvent() is sampling ${tempEvent.type} siteID: ${NeuroID.linkedSiteID}")
        val event =
            tempEvent.copy(
                ts = System.currentTimeMillis(),
                gyro = NIDSensorHelper.getGyroscopeInfo(),
                accel = NIDSensorHelper.getAccelerometerInfo(),
            )

        queuedEvents.add(event)
    }

    @Synchronized
    override fun saveAndClearAllQueuedEvents() {
        eventsList.addAll(queuedEvents)
        queuedEvents.clear()
    }

    /*
        Returns a Boolean indicating if the event was successfully saved or not
     */
    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        if (!samplingService.isSessionFlowSampled()) {
            logger.i(msg="NID saveEvent() is not sampling ${event.type} siteID: ${NeuroID.linkedSiteID}")
            return
        }
        logger.i(msg="NID saveEvent() is sampling ${event.type} siteID: ${NeuroID.linkedSiteID}")
        if (!listNonActiveEvents.contains(event.type)) {
            NIDTimerActive.restartTimerActive()
        }

        // capture the event
        eventsList.add(event)
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
                previousEventsList[index] =
                    item.copy(
                        accel = accel,
                        gyro = gyro,
                        url = url,
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

    override fun isFullBuffer(): Boolean {
        val lastEventType = if (eventsList.isEmpty()) "" else eventsList.last().type

        if (lastEventType == FULL_BUFFER) {
            return true
        } else if ((eventsList.size + queuedEvents.size) > configService.getRemoteNIDConfig().eventQueueFlushSize) {
            // add a full buffer event and drop the new event
            saveEvent(
                NIDEventModel(type = FULL_BUFFER, ts = System.currentTimeMillis()),
            )
            return true
        }

        return false
    }
}
