package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NIDJobServiceManager {

    internal var jobCaptureEvents: Job? = null
    var isSendEventsNowEnabled = true

    @Volatile
    var userActive = true
    internal var clientKey = ""
    private var application: Application? = null
    internal var endpoint: String = ""

    internal var isSetup: Boolean = false

    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
        endpoint: String
    ) {
        this.clientKey = clientKey
        this.endpoint = endpoint
        this.application = application
        jobCaptureEvents = createJobServer()
        NIDSensorHelper.initSensorHelper(application, NIDLogWrapper())
        this.isSetup = true
    }

    @Synchronized
    fun stopJob() {
        NIDSensorHelper.stopSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
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
    }

    private fun createJobServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive && isActive) {
                delay(5000L)
                sendEventsNow(NIDLogWrapper())
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
                    .addInterceptor(LoggerIntercepter(NIDLogWrapper())).build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NIDApiService::class.java)
    )

    /**
     * The timeouts values are defaults from the OKHttp and can be modified as needed. These are
     * set here to show what timeouts are available in the OKHttp client.
     */
    suspend fun sendEventsNow(logger: NIDLogWrapper, forceSendEvents: Boolean = false,
                              eventSender: NIDEventSender = getServiceAPI(),
                              dataStoreManager: NIDDataStoreManager = getDataStoreInstance()
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
                    dataStoreManager
                )
            } ?: run {
                userActive = false
            }
        }
    }
}