package com.neuroid.tracker.service

import android.content.Context
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST_FAILED
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface AdvancedDeviceIDManagerService {
    fun getCachedID(): Boolean

    fun getRemoteID(
        clientKey: String,
        remoteUrl: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        delay: Long = 5000L,
    ): Job?
}

internal class AdvancedDeviceIDManager(
    private val context: Context,
    private val logger: NIDLogWrapper,
    private val sharedPrefs: NIDSharedPrefsDefaults,
    private val neuroID: NeuroID,
    private val advNetworkService: ADVNetworkService,
    private val clientID: String,
    private val linkedSiteID: String,
    // only for testing purposes, need to create in real time to pass NID Key
    private val fpjsClient: FingerprintJS? = null,
) : AdvancedDeviceIDManagerService {
    companion object {
        internal val NID_RID = "NID_RID_KEY"
        internal val defaultCacheValue = "{\"key\":\"NO_KEY\", \"exp\":0}"
    }

    override fun getCachedID(): Boolean {
        val existingString = sharedPrefs.getString(NID_RID, defaultCacheValue)

        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val storedValue = gson.fromJson<Map<String, Any>>(existingString, type)

        // No Value Exists - return false
        if (storedValue == null || storedValue["key"] == "NO_KEY") {
            return false
        }

        // Expired ID - return false
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp > storedValue["exp"] as Double) {
            return false
        }

        // Capture valid cached ID
        logger.d(
            msg =
                "Retrieving Request ID for Advanced Device Signals from cache: ${storedValue["key"]}",
        )
        neuroID.captureEvent(
            type = ADVANCED_DEVICE_REQUEST,
            rid = storedValue["key"] as String,
            ts = System.currentTimeMillis(),
            c = true,
            l = 0,
            // wifi/cell
            ct = neuroID.networkConnectionType,
        )

        return true
    }

    override fun getRemoteID(
        clientKey: String,
        remoteUrl: String,
        dispatcher: CoroutineDispatcher,
        delay: Long,
    ): Job? {
        // Retrieving the API key from NeuroID
        val nidKeyResponse = advNetworkService.getNIDAdvancedDeviceAccessKey(clientKey, clientID, linkedSiteID)

        // if no key exit early
        if (!nidKeyResponse.success) {
            logger.e(msg = "Failed to get API key from NeuroID: ${nidKeyResponse.message}")

            // capture failure in event
            neuroID.captureEvent(
                type = LOG,
                ts = System.currentTimeMillis(),
                level = "error",
                m = nidKeyResponse.message,
            )

            neuroID.captureEvent(
                type = ADVANCED_DEVICE_REQUEST_FAILED,
                ts = System.currentTimeMillis(),
                m = nidKeyResponse.message,
            )
            return null
        }

        // Call FPJS with our key
        val fpjsClient =
            if (fpjsClient != null) {
                fpjsClient
            } else {
                FingerprintJSFactory(applicationContext = context)
                    .createInstance(
                        Configuration(
                            apiKey = nidKeyResponse.key,
                            endpointUrl = remoteUrl,
                        ),
                    )
            }

        val remoteIDJob =
            CoroutineScope(dispatcher).launch {
                val maxRetryCount = NIDAdvancedDeviceNetworkService.RETRY_COUNT

                var jobComplete = false
                var jobErrorMessage = ""
                for (retryCount in 1..maxRetryCount) {
                    // we want to see the latency from FPJS on each reuest
                    val startTime = Calendar.getInstance().timeInMillis

                    // returns a Bool - success, String - key OR error message
                    val requestResponse = getVisitorId(fpjsClient)

                    // If Success - capture ID and cache, end loop
                    if (requestResponse.first) {
                        logger.d(
                            msg =
                                "Generating Request ID for Advanced Device Signals: ${requestResponse.second}",
                        )

                        val stopTime = Calendar.getInstance().timeInMillis
                        neuroID.captureEvent(
                            type = ADVANCED_DEVICE_REQUEST,
                            rid = requestResponse.second,
                            ts = Calendar.getInstance().timeInMillis,
                            c = false,
                            // time start to end time
                            l = stopTime - startTime,
                            // wifi/cell
                            ct = neuroID.networkConnectionType,
                        )

                        logger.d(msg = "Caching Request ID: ${requestResponse.second}")
                        // Cache request Id for 24 hours
                        //  and convert the Map to a JSON String
                        val gson = Gson()
                        val cachedValueString =
                            gson.toJson(
                                mapOf(
                                    "key" to requestResponse.second,
                                    "exp" to
                                        (24 * 60 * 60 * 1000) +
                                        System.currentTimeMillis(), // 24
                                    // hours from now
                                ),
                            )
                        sharedPrefs.putString(NID_RID, cachedValueString)

                        // end while loop
                        jobComplete = true
                        break
                    }

                    // If Failure - retry until max retry, then log failure in event
                    jobErrorMessage = requestResponse.second
                    logger.d(
                        msg =
                            "Error retrieving Advanced Device Signal Request ID:$jobErrorMessage: $retryCount",
                    )
                    Thread.sleep(delay)
                }

                if (!jobComplete) {
                    val msg =
                        "Reached maximum number of retries ($maxRetryCount) to get Advanced Device Signal Request ID: $jobErrorMessage"

                    neuroID.captureEvent(
                        type = LOG,
                        ts = Calendar.getInstance().timeInMillis,
                        level = "error",
                        m = msg,
                    )
                    logger.e(msg = msg)
                }
            }

        return remoteIDJob
    }

    private suspend fun getVisitorId(fpjsClient: FingerprintJS): Pair<Boolean, String> =
        suspendCoroutine { continuation ->
            fpjsClient.getVisitorId(
                listener = { result ->
                    continuation.resume(Pair(true, result.requestId))
                },
                errorListener = { error ->
                    continuation.resume(
                        Pair(
                            false,
                            if (error.description != null) {
                                error.description as String
                            } else {
                                "FPJS Empty Failure"
                            },
                        ),
                    )
                },
            )
        }
}
