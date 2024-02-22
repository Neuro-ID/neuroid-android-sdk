package com.neuroid.tracker.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroID.Companion.GYRO_SAMPLE_INTERVAL
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.CADENCE_READING_ACCEL
import com.neuroid.tracker.events.LOW_MEMORY
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NIDJobServiceManager(
    private var logger:NIDLogWrapper,
    private var dataStore:NIDDataStoreManager,
    private var eventSender: NIDSendingService
)
{
    @Volatile
    var userActive = true
    var isSendEventsNowEnabled = true

    internal var jobCaptureEvents: Job? = null
    internal var gyroCadenceJob: Job? = null

    internal var clientKey = ""
    internal var isSetup: Boolean = false

    private var application: Application? = null
    private var activityManager: ActivityManager? = null


    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
    ) {
        this.userActive = true
        this.clientKey = clientKey
        this.application = application
        jobCaptureEvents = createJobServer()
        NIDSensorHelper.initSensorHelper(application, logger)

        gyroCadenceJob = createGyroJobServer()
        activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        this.isSetup = true
    }

    @Synchronized
    fun stopJob() {
        NIDSensorHelper.stopSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null

        gyroCadenceJob?.cancel()
        gyroCadenceJob = null
    }

    @Synchronized
    fun isStopped(): Boolean {
        return jobCaptureEvents?.isActive != true
    }

    @Synchronized
    fun restart() {
        this.userActive = true
        NIDSensorHelper.restartSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = createJobServer()

        gyroCadenceJob?.cancel()
        gyroCadenceJob = createGyroJobServer()
    }

    private fun createJobServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive && isActive) {
                delay(5000L)
                sendEventsNow()
            }
        }
    }

    private fun createGyroJobServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (NeuroID.isSDKStarted && NeuroID.captureGyroCadence) {
                delay(NeuroID.GYRO_SAMPLE_INTERVAL)

                dataStore.saveEvent(
                    NIDEventModel(
                        type = CADENCE_READING_ACCEL,
                        attrs = listOf(
                            mapOf(
                                "interval" to "${1000 * GYRO_SAMPLE_INTERVAL}s"
                            )
                        )
                    )
                )

            }
        }
    }

    /**
     * The timeouts values are defaults from the OKHttp and can be modified as needed. These are
     * set here to show what timeouts are available in the OKHttp client.
     */
    suspend fun sendEventsNow(
        forceSendEvents: Boolean = false
    ) {
        if (isSendEventsNowEnabled && (forceSendEvents || !isStopped())) {
            application?.let {
                eventSender.sendEvents(
                    clientKey,
                    getEventsToSend(it),
                    object: NIDResponseCallBack {
                        override fun onSuccess(code: Int) {
                            logger.d(msg = " network success, sendEventsNow() success userActive: $userActive")
                        }

                        override fun onFailure(code: Int, message: String, isRetry: Boolean) {
                            logger.e(msg = "network failure, sendEventsNow() failed retrylimitHit: ${!isRetry} $message")
                        }
                    }
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

        NeuroID.getInstance()?.lowMemory = memoryInfo.lowMemory

        if (memoryInfo.lowMemory) {
            return NIDEventModel(
                type = LOW_MEMORY,
                ts = System.currentTimeMillis(),
                attrs = listOf(
                    mapOf<String, Any>(
                        "isLowMemory" to memoryInfo.lowMemory,
                        "total" to memoryInfo.totalMem,
                        "available" to memoryInfo.availMem,
                        "threshold" to memoryInfo.threshold,
                    )
                )
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

        if (lowMemEvent !=null) {
            // return the low memory event to be sent
            return listOf(lowMemEvent)
        }

        return dataStore.getAllEvents()
    }

    @VisibleForTesting
    internal fun setTestEventSender(eventSender: NIDSendingService){
        this.eventSender = eventSender
    }
}