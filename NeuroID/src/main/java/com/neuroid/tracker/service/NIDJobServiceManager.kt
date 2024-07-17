package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.CADENCE_READING_ACCEL
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.LOW_MEMORY
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class NIDJobServiceManager(
    private var neuroID: NeuroID,
    private var dataStore: NIDDataStoreManager,
    private var eventSender: NIDSendingService,
    private var logger: NIDLogWrapper,
    private var configService: ConfigService,
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @Volatile
    var userActive = true
    var isSendEventsNowEnabled = true

    internal var clientKey = ""
    internal var isSetup: Boolean = false

    internal var sendCadenceJob: Job? = null
    internal var gyroCadenceJob: Job? = null

    private val sendEventsNotification = Channel<Boolean>(Channel.UNLIMITED)
    private var application: Application? = null
    private var activityManager: ActivityManager? = null
    private var sendEventsJob: Job = createSendEventsServer()

    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
    ) {
        this.userActive = true
        this.clientKey = clientKey
        this.application = application

        sendEventsJob = createSendEventsServer()
        sendCadenceJob = createSendCadenceServer()
        NIDSensorHelper.initSensorHelper(application, logger)

        gyroCadenceJob = createGyroCadenceServer()
        activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        this.isSetup = true
    }

    @Synchronized
    fun stopJob() {
        NIDSensorHelper.stopSensors()
        sendCadenceJob?.cancel()
        sendCadenceJob = null

        gyroCadenceJob?.cancel()
        gyroCadenceJob = null
    }

    @Synchronized
    fun isStopped(): Boolean {
        return sendCadenceJob?.isActive != true
    }

    @Synchronized
    fun restart() {
        this.userActive = true
        NIDSensorHelper.restartSensors()

        // If the job has ended for any reason restart it
        if (!sendEventsJob.isActive) {
            sendEventsJob.cancel()
            sendEventsJob = createSendEventsServer()
        }

        sendCadenceJob?.cancel()
        sendCadenceJob = createSendCadenceServer()

        gyroCadenceJob?.cancel()
        gyroCadenceJob = createGyroCadenceServer()
    }

    /**
     * Notify the send events server to initiate a send.
     */
    fun sendEvents(forceSendEvents: Boolean = false) {
        sendEventsNotification.trySend(forceSendEvents)
    }

    /**
     * Creates a job that synchronizes all requests to send events.
     *   The function will loop through all the current sendEventNotifications
     *   and collapse them into one request to be sent rather than multiple
     */
    private fun createSendEventsServer(): Job {
        return CoroutineScope(dispatcher).launch {
            try {
                for (notification in sendEventsNotification) {
                    var sendNow = notification
                    var next = sendEventsNotification.tryReceive()
                    while (next.isSuccess) {
                        next.getOrNull()?.let {
                            sendNow = sendNow || it
                        }
                        next = sendEventsNotification.tryReceive()
                    }
                    sendEventsNow(sendNow)
                }
            } catch (exception: Exception) {
                logger.e("NeuroID", exception.toString())
            }
            neuroID.captureEvent(type = LOG, m = "Send Event Job Exited", level = "ERROR")
            logger.e("NeuroID", "Send Event Job Exited")
        }
    }

    /**
     * Creates a job that notifies the sendEvents channel to send events
     */
    private fun createSendCadenceServer(): Job {
        return CoroutineScope(dispatcher).launch {
            while (userActive && isActive) {
                delay(configService.configCache.eventQueueFlushInterval * 1000L)
                sendEventsNotification.send(false)
            }
        }
    }

    /**
     * Create a job that captures the gyro/accel data in an event on a set cadence
     */
    private fun createGyroCadenceServer(): Job {
        return CoroutineScope(dispatcher).launch {
            while (NeuroID.isSDKStarted && configService.configCache.gyroAccelCadence) {
                delay(configService.configCache.gyroAccelCadenceTime)

                neuroID.captureEvent(
                    type = CADENCE_READING_ACCEL,
                    attrs =
                        listOf(
                            mapOf(
                                "interval" to "${configService.configCache.gyroAccelCadenceTime}sec",
                            ),
                        ),
                )
            }
        }
    }

    /**
     * The timeouts values are defaults from the OKHttp and can be modified as needed. These are
     * set here to show what timeouts are available in the OKHttp client.
     */
    private fun sendEventsNow(forceSendEvents: Boolean = false) {
        if (isSendEventsNowEnabled && (forceSendEvents || !isStopped())) {
            application?.let {
                eventSender.sendEvents(
                    clientKey,
                    getEventsToSend(it),
                    object : NIDResponseCallBack<Any> {
                        override fun onSuccess(
                            code: Int,
                            response: Any,
                        ) {
                            neuroID.incrementPacketNumber()
                            logger.d(msg = " network success, sendEventsNow() success userActive: $userActive")
                        }

                        override fun onFailure(
                            code: Int,
                            message: String,
                            isRetry: Boolean,
                        ) {
                            neuroID.captureEvent(type = LOG, m = "network failure, sendEventsNow() failed retrylimitHit: $message $code")
                            logger.e(msg = "network failure, sendEventsNow() failed retrylimitHit: ${!isRetry} $message")
                        }
                    },
                )
            } ?: run {
                userActive = false
            }
        }
    }

    /**
     * Get the current system memory state.
     */
    private fun checkMemoryLevel(context: Context): NIDEventModel? {
        val memoryInfo = ActivityManager.MemoryInfo()

        // shouldn't ever be null because it is initialized with startJob, but putting this here as a safety check
        if (activityManager == null) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
        } else {
            activityManager?.getMemoryInfo(memoryInfo)
        }

        NeuroID.getInternalInstance()?.lowMemory = memoryInfo.lowMemory

        if (memoryInfo.lowMemory) {
            return NIDEventModel(
                type = LOW_MEMORY,
                ts = System.currentTimeMillis(),
                attrs =
                    listOf(
                        mapOf<String, Any>(
                            "isLowMemory" to memoryInfo.lowMemory,
                            "total" to memoryInfo.totalMem,
                            "available" to memoryInfo.availMem,
                            "threshold" to memoryInfo.threshold,
                        ),
                    ),
            )
        }

        return null
    }

    /**
     * Get the events that should be sent.  The 2 scenarios are:
     * 1. if system memory is low, create a low memory event and return that
     * 2. get the current list of events from the data store manager
     */
    private fun getEventsToSend(context: Context): List<NIDEventModel> {
        val lowMemEvent = checkMemoryLevel(context)

        if (lowMemEvent != null) {
            // return the low memory event to be sent
            return listOf(lowMemEvent)
        }

        return dataStore.getAllEvents()
    }

    @VisibleForTesting
    internal fun setTestEventSender(eventSender: NIDSendingService) {
        this.eventSender = eventSender
    }
}
