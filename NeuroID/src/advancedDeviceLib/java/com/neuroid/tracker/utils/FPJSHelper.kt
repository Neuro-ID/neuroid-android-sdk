package com.neuroid.tracker.utils

import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults

class FPJSHelper {
    fun createRequestIdEvent(
        fpjsClient: FingerprintJS?,
        fpjsRetryCount: Int, maxRetryCount: Int,
        sharedPrefs: NIDSharedPrefsDefaults,
        nidLogger: NIDLogWrapper,
        dataStoreManager: NIDDataStoreManager
    ) {
        var retryCount = fpjsRetryCount

        // Check if request id is in cache and has expired
        if (sharedPrefs.hasRequestIdExpired()) {
            nidLogger.d(
                msg = "Request ID not found in cache"
            )
            // Request Id from server
            fpjsClient?.getVisitorId(listener = { result ->
                nidLogger.d(
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
                nidLogger.d(
                    msg = "Caching Request ID: ${result.requestId}"
                )
                sharedPrefs.cacheRequestId(result.requestId)
                sharedPrefs.cacheRequestIdExpirationTimestamp()
            },
                errorListener = { error ->
                    retryCount += 1
                    nidLogger.d(
                        msg = "Error retrieving Advanced Device Signal Request ID:${error.description}: $fpjsRetryCount"
                    )
                    Thread.sleep(5000)
                    if (retryCount < maxRetryCount) {
                        createRequestIdEvent(
                            fpjsClient, retryCount, maxRetryCount,
                            sharedPrefs, nidLogger, dataStoreManager
                        )
                    } else {
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
                        nidLogger.e(
                            msg = msg
                        )
                    }

                })
        } else {
            //  Retrieve request id from cache
            val requestId = sharedPrefs.getCachedRequestId()
            nidLogger.d(
                msg = "Retrieving Request ID for Advanced Device Signals from cache: ${requestId}"
            )
            dataStoreManager.saveEvent(
                NIDEventModel(
                    type = ADVANCED_DEVICE_REQUEST,
                    rid = requestId,
                    ts = System.currentTimeMillis(),
                    c = true
                )
            )
        }
    }
}