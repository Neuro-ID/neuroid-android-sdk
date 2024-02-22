package com.neuroid.tracker.service

import android.content.Context
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface AdvancedDeviceIDManagerService {
    fun getCachedID(): Boolean
    fun getRemoteID(clientKey: String, remoteUrl: String): Job?
}

internal class AdvancedDeviceIDManager(
        private val context: Context,
        private val logger: NIDLogWrapper,
        private val sharedPrefs: NIDSharedPrefsDefaults,
        private val dataStoreManager: NIDDataStoreManager,
        private val advNetworkService: ADVNetworkService,
        private val fpjsClient:
                FingerprintJS? // only for testing purposes, need to create in real time to pass NID
                               //   Key
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
                        "Retrieving Request ID for Advanced Device Signals from cache: ${storedValue["key"]}"
        )
        dataStoreManager.saveEvent(
                NIDEventModel(
                        type = ADVANCED_DEVICE_REQUEST,
                        rid = storedValue["key"] as String,
                        ts = System.currentTimeMillis(),
                        c = true
                )
        )

        return true
    }

    override fun getRemoteID(clientKey: String, remoteUrl: String): Job? {
        // Retrieving the API key from NeuroID
        val nidKeyResponse = advNetworkService.getNIDAdvancedDeviceAccessKey(clientKey)

        // if no key exit early
        if (!nidKeyResponse.success) {
            logger.e(msg = "Failed to get API key from NeuroID: ${nidKeyResponse.message}")

            // capture failure in event
            dataStoreManager.saveEvent(
                    NIDEventModel(
                            type = LOG,
                            ts = System.currentTimeMillis(),
                            level = "error",
                            m = nidKeyResponse.message
                    )
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
                                            endpointUrl = remoteUrl
                                    )
                            )
                }

        val remoteIDJob =
                CoroutineScope(Dispatchers.IO).launch {
                    val maxRetryCount = NIDAdvancedDeviceNetworkService.RETRY_COUNT

                    for (retryCount in 1..maxRetryCount) {
                        // returns a Bool - success, String - key OR error message
                        val requestResponse = getVisitorId(fpjsClient)

                        // If Success - capture ID and cache, end loop
                        if (requestResponse.first) {
                            logger.d(
                                    msg =
                                            "Generating Request ID for Advanced Device Signals: ${requestResponse.second}"
                            )

                            dataStoreManager.saveEvent(
                                    NIDEventModel(
                                            type = ADVANCED_DEVICE_REQUEST,
                                            rid = requestResponse.second,
                                            ts = System.currentTimeMillis(),
                                            c = false
                                    )
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
                                                                    System.currentTimeMillis() // 24
                                                    // hours from now
                                                    )
                                    )
                            sharedPrefs.putString(NID_RID, cachedValueString)

                            // end while loop
                            break
                        }

                        // If Failure - retry until max retry, then log failure in event
                        logger.d(
                                msg =
                                        "Error retrieving Advanced Device Signal Request ID:${requestResponse.second}: $retryCount"
                        )
                        Thread.sleep(5000)

                        if (!(retryCount+1 < maxRetryCount)) {
                            val msg =
                                    "Reached maximum number of retries ($maxRetryCount) to get Advanced Device Signal Request ID:${requestResponse.second}"

                            dataStoreManager.saveEvent(
                                    NIDEventModel(
                                            type = LOG,
                                            ts = System.currentTimeMillis(),
                                            level = "error",
                                            m = msg
                                    )
                            )
                            logger.e(msg = msg)
                        }
                    }

                }

        return remoteIDJob
    }

    private suspend fun getVisitorId(fpjsClient: FingerprintJS): Pair<Boolean, String> =
            suspendCoroutine { continuation ->
                fpjsClient.getVisitorId(
                        listener = { result -> continuation.resume(Pair(true, result.requestId)) },
                        errorListener = { error ->
                            continuation.resume(
                                    Pair(
                                            false,
                                            if (error.description != null) {
                                                error.description as String
                                            } else {
                                                "FPJS Empty Failure"
                                            }
                                    )
                            )
                        }
                )
            }
}
