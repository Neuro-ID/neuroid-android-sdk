package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroID.Companion.GYRO_SAMPLE_INTERVAL
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.CADENCE_READING_ACCEL
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NIDJobServiceManager(
    private var logger:NIDLogWrapper,
    private var dataStore:NIDDataStoreManager
)
{
    @Volatile
    var userActive = true
    var isSendEventsNowEnabled = true

    internal var jobCaptureEvents: Job? = null
    internal var gyroCadenceJob: Job? = null

    internal var clientKey = ""
    internal var endpoint = Constants.productionEndpoint.displayName
    internal var isSetup: Boolean = false

    private var application: Application? = null


    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
    ) {
        this.clientKey = clientKey
        this.application = application
        jobCaptureEvents = createJobServer()
        NIDSensorHelper.initSensorHelper(application, logger)

        gyroCadenceJob = createGyroJobServer()
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
                if(NIDSensorHelper.isSensorActive()){
                    NIDSensorHelper.restartSensors()
                }
                delay(NeuroID.GYRO_SAMPLE_INTERVAL)

                val gyroData = NIDSensorHelper.getGyroscopeInfo()
                val accelData = NIDSensorHelper.getAccelerometerInfo()
                val attrsObj = JSONObject().put("interval", "${1000 * GYRO_SAMPLE_INTERVAL}s")

                dataStore.saveEvent(
                    NIDEventModel(
                        type = CADENCE_READING_ACCEL,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        attrs = JSONArray().put(attrsObj)
                    )
                )

            }
        }
    }

    // TODO: move this out of the job manager when we refactor the /a advanced key endpoint grabber
    fun getServiceAPI() = NIDEventSender(
        Retrofit.Builder()
            .baseUrl(endpoint)
            .client(
                OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(0, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .addInterceptor(LoggerIntercepter(logger)).build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NIDApiService::class.java)
    )

    /**
     * The timeouts values are defaults from the OKHttp and can be modified as needed. These are
     * set here to show what timeouts are available in the OKHttp client.
     */
    suspend fun sendEventsNow(
        forceSendEvents: Boolean = false,
        eventSender: NIDEventSender = getServiceAPI(),
    ) {
        if (forceSendEvents || (isSendEventsNowEnabled && !isStopped())) {
            application?.let {
                NIDServiceTracker.sendEventToServer(
                    eventSender,
                    clientKey,
                    it,
                    null,
                    object: NIDResponseCallBack {
                        override fun onSuccess(code: Int) {
                            // noop!
                            logger.d(msg = " network success, sendEventsNow() success userActive: $userActive")
                        }

                        override fun onFailure(code: Int, message: String, isRetry: Boolean) {
                            // if isRetry = false, then the retry probably hit the retry limit so we
                            // kill the job manager, isRetry = true if the retry
                            // logic is still trying.
                            userActive = isRetry
                            logger.e(msg = "network failure, sendEventsNow() failed userActive: $userActive $message")
                        }
                    },
                    dataStore
                )
            } ?: run {
                userActive = false
            }
        }
    }
}