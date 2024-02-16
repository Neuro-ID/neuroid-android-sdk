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

interface AdvancedDeviceIDManagerService {
    fun getCachedID():Boolean
    fun getRemoteID(clientKey: String, remoteUrl: String)
}

internal class AdvancedDeviceIDManager(
    private val context:Context,
    private val logger:NIDLogWrapper,
    private val sharedPrefs: NIDSharedPrefsDefaults,
    private val dataStoreManager: NIDDataStoreManager,
    private val advNetworkService: ADVNetworkService,
    private val fpjsClient: FingerprintJS? // only for testing purposes, need to create in real time to pass NID Key
): AdvancedDeviceIDManagerService {
    companion object {
        internal val NID_RID = "NID_RID_KEY"
    }

    override fun getCachedID():Boolean{
        val existingString = sharedPrefs.getString(NID_RID, "{\"key\":\"NO_KEY\", \"exp\":0}")

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
            msg = "Retrieving Request ID for Advanced Device Signals from cache: ${storedValue["key"]}"
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

    override fun getRemoteID(
        clientKey:String,
        remoteUrl:String
    ){
        // Retrieving the API key from NeuroID
        val nidKeyResponse = advNetworkService.getADVKey(clientKey)

        // if no key exit early
        if(!nidKeyResponse.success) {
            logger.e(msg = "Failed to get API key from NeuroID: ${nidKeyResponse.message}")

            // capture failure in event
            dataStoreManager
                .saveEvent(
                    NIDEventModel(
                        type = LOG,
                        ts = System.currentTimeMillis(),
                        level="error",
                        m=nidKeyResponse.message
                    )
                )
            return
        }

        // Call FPJS with our key
        val fpjsClient = if( fpjsClient != null) {
            fpjsClient
        } else {
            FingerprintJSFactory(applicationContext = context)
                .createInstance(
                    Configuration(
                        apiKey = clientKey,
                        endpointUrl = remoteUrl
                    )
                )
        }

        var maxRetryCount = NIDAdvancedDeviceNetworkService.RETRY_COUNT
        var retryCount = 0
        while (retryCount < maxRetryCount) {
            // Two Callbacks - Success & Error
            //  - If Success - capture ID and cache, end loop
            //  - If Failure - retry until max retry, then log failure in event
            fpjsClient.getVisitorId(
                listener = { result ->
                    logger.d(
                        msg = "Generating Request ID for Advanced Device Signals: ${result.requestId}"
                    )

                    dataStoreManager.saveEvent(
                        NIDEventModel(
                            type = ADVANCED_DEVICE_REQUEST,
                            rid = result.requestId,
                            ts = System.currentTimeMillis(),
                            c = false
                        )
                    )
                    // Cache request Id for 24 hours
                    logger.d(
                        msg = "Caching Request ID: ${result.requestId}"
                    )

                    // Convert the Map to a JSON String
                    val gson = Gson()
                    val cachedValueString = gson.toJson(
                        mapOf(
                            "key" to result.requestId,
                            "exp" to (24 * 60 * 60 * 1000) + System.currentTimeMillis() // 24 hours from now
                        )
                    )

                    sharedPrefs.putString(NID_RID, cachedValueString)
                    // end while loop
                    retryCount = maxRetryCount + 1
                },
                errorListener = { error ->
                    retryCount += 1
                    logger.d(
                        msg = "Error retrieving Advanced Device Signal Request ID:${error.description}: $retryCount"
                    )
                    Thread.sleep(5000)

                    if (!(retryCount < maxRetryCount)) {
                        val msg =
                            "Reached maximum number of retries ($maxRetryCount) to get Advanced Device Signal Request ID:${error.description}"

                        dataStoreManager.saveEvent(
                            NIDEventModel(
                                type = LOG,
                                ts = System.currentTimeMillis(),
                                level = "error",
                                m = msg
                            )
                        )
                        logger.e(
                            msg = msg
                        )
                    }
                }
            )
        }
    }
}