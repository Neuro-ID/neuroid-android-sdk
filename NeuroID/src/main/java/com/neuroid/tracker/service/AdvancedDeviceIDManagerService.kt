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
import com.neuroid.tracker.models.ADVKeyFunctionResponse
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
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
        remoteUrlProd: String,
        remoteUrlProxyPrimary: String,
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
    private val configService: ConfigService,
    private val advancedDeviceKey: String? = null,
    // only for testing purposes, need to create in real time to pass NID Key
    private val fpjsClient: FingerprintJS? = null,
    private val isFPJSProxyEnabled: Boolean,
    val nidTime: NIDTime = NIDTime()
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
            queuedEvent = true,
            type = ADVANCED_DEVICE_REQUEST,
            rid = storedValue["key"] as String,
            scr = storedValue["scr"] as String?,
            c = true,
            l = 0,
            // wifi/cell
            ct = neuroID.networkConnectionType,
        )

        return true
    }

    fun getAdvancedDeviceKey(clientKey: String): ADVKeyFunctionResponse? {
        val nidKeyResponse =
            advNetworkService.getNIDAdvancedDeviceAccessKey(clientKey, clientID, linkedSiteID)

        // if no key exit early
        if (!nidKeyResponse.success) {
            logger.e(msg = "Failed to get API key from NeuroID: ${nidKeyResponse.message}")

            // capture failure in event
            neuroID.captureEvent(
                queuedEvent = true,
                type = LOG,
                ts = nidTime.getCurrentTimeMillis(),
                level = "error",
                m = nidKeyResponse.message,
            )
            return null
        }
        return nidKeyResponse
    }

    internal fun chooseUrl(isFPJSProxyEnabled: Boolean,
                           remoteUrlProxyPrimary: String,
                     remoteProdUrl: String): String {
        return if (isFPJSProxyEnabled) {
            remoteUrlProxyPrimary
        } else {
            remoteProdUrl
        }
    }

    override fun getRemoteID(
        clientKey: String,
        remoteProdUrl: String,
        remoteUrlProxyPrimary: String,
        dispatcher: CoroutineDispatcher,
        delay: Long
    ): Job? {
        // check if we have a user entered FPJS key,
        // if not, get it from server
        var fpjsRetrievedKey = ""
        if (advancedDeviceKey.isNullOrEmpty()) {
            val keyFunctionResponse = getAdvancedDeviceKey(clientKey)
            // if server gotten FPJS key is null or empty exit immediately
            if (keyFunctionResponse == null) {
                return null
            } else {
                // set the key for use later if successfully gotten from server.
                keyFunctionResponse?.let {
                    fpjsRetrievedKey = keyFunctionResponse.key
                }
            }
        }

        // Call FPJS with our key (user entered or server retrieved)
        val fpjsClient =
            if (fpjsClient != null) {
                fpjsClient
            } else {
                FingerprintJSFactory(applicationContext = context)
                    .createInstance(
                        Configuration(
                            apiKey = if (!advancedDeviceKey.isNullOrEmpty()) advancedDeviceKey else fpjsRetrievedKey,
                            // endpointUrl = if (isFPJSProxyEnabled) remoteUrlProxyPrimary else remoteProdUrl,
                            endpointUrl = chooseUrl(isFPJSProxyEnabled, remoteUrlProxyPrimary, remoteProdUrl),
                            fallbackEndpointUrls = arrayListOf(remoteProdUrl)
                        ),
                    )
            }

        val remoteIDJob =
            CoroutineScope(dispatcher).launch {
                val maxRetryCount = NIDAdvancedDeviceNetworkService.RETRY_COUNT

                var jobComplete = false
                var jobErrorMessage = ""
                for (retryCount in 1..maxRetryCount) {
                    // we want to see the latency from FPJS on each request
                    val startTime = nidTime.getCurrentTimeMillis()
                    // returns a Bool - success, String - key OR error message
                    val requestResponse = getVisitorId(fpjsClient)
                    // If Success - capture ID and cache, end loop
                    if (requestResponse.first) {
                        logger.d(
                            msg =
                                "Generating Request ID for Advanced Device Signals: ${requestResponse.second}",
                        )

                        val stopTime = nidTime.getCurrentTimeMillis()
                        neuroID.captureEvent(
                            queuedEvent = true,
                            type = ADVANCED_DEVICE_REQUEST,
                            rid = requestResponse.second,
                            ts = nidTime.getCurrentTimeMillis(),
                            c = false,
                            // time start to end time
                            l = stopTime - startTime,
                            // wifi/cell
                            ct = neuroID.networkConnectionType,
                            scr = requestResponse.third,
                            m = if (advancedDeviceKey.isNullOrEmpty()) "server retrieved FPJS key" else "user entered FPJS key"
                        )
                        logger.d(msg = "Caching Request ID: ${requestResponse.second}")
                        // Cache request Id for 24 hours
                        //  and convert the Map to a JSON String
                        val gson = Gson()
                        val cachedValueString =
                            gson.toJson(
                                mapOf(
                                    "scr" to requestResponse.third,
                                    "key" to requestResponse.second,
                                    "exp" to
                                        (configService.configCache.advancedCookieExpiration * 1000) +
                                        nidTime.getCurrentTimeMillis(), // 12
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
                        queuedEvent = true,
                        type = LOG,
                        ts = nidTime.getCurrentTimeMillis(),
                        level = "error",
                        m = msg,
                    )
                    neuroID.captureEvent(
                        queuedEvent = true,
                        type = ADVANCED_DEVICE_REQUEST_FAILED,
                        ts = nidTime.getCurrentTimeMillis(),
                        m = msg,
                    )
                    logger.e(msg = msg)
                }
            }

        return remoteIDJob
    }

    private suspend fun getVisitorId(fpjsClient: FingerprintJS): Triple<Boolean, String, String?> =
        suspendCoroutine { continuation ->
            fpjsClient.getVisitorId(
                listener = { result ->
                    continuation.resume(Triple(true, result.requestId, result.sealedResult))
                },
                errorListener = { error ->
                    continuation.resume(
                        Triple(
                            false,
                            if (error.description != null) {
                                error.description as String
                            } else {
                                "FPJS Empty Failure"
                            }, null
                        ),
                    )
                },
            )
        }
}
