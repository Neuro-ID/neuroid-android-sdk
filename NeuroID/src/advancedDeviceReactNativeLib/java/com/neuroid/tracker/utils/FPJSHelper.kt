package com.neuroid.tracker.utils

import android.content.Context
import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance

class FPJSHelper(private val context: Context) {
    fun createRequestIdEvent(fpjsClient: FingerprintJS?, fpjsRetryCount: Int, maxRetryCount: Int) {
        var retryCount = fpjsRetryCount

        // Check if request id is in cache and has expired
        val sharedDefaults = NIDSharedPrefsDefaults(context)
        if (sharedDefaults.hasRequestIdExpired()) {
            NIDLog.d(              
                msg="Request ID not found in cache"
            )
            // Request Id from server
            fpjsClient?.getVisitorId(listener = { result ->
                NIDLog.d(                  
                    msg="Generating Request ID for Advanced Device Signals: ${result.requestId}"
                )
                getDataStoreInstance().saveEvent(
                    NIDEventModel(
                        type = ADVANCED_DEVICE_REQUEST,
                        rid = result.requestId,
                        ts = System.currentTimeMillis(),
                        c = false
                    )
                )
                // Cache request Id for 24 hours
                NIDLog.d(                  
                    msg="Caching Request ID: ${result.requestId}"
                )
                sharedDefaults.cacheRequestId(result.requestId)
                sharedDefaults.cacheRequestIdExpirationTimestamp()
            },
                errorListener = { error ->
                    retryCount += 1
                    NIDLog.d(                      
                        msg="Error retrieving Advanced Device Signal Request ID:${error.description}: $fpjsRetryCount"
                    )
                    Thread.sleep(5000)
                    if (retryCount < maxRetryCount) {
                        createRequestIdEvent(fpjsClient, retryCount, maxRetryCount)
                    } else {
                        getDataStoreInstance().saveEvent(
                            NIDEventModel(
                                type = LOG,
                                ts = System.currentTimeMillis(),
                                level="error",
                                m="Reached maximum number of retries ($maxRetryCount) to get Advanced Device Signal Request ID:${error.description}"
                            )
                        )
                        NIDLog.e(
                            msg="Reached maximum number of retries ($maxRetryCount) to get Advanced Device Signal Request ID:${error.description}"
                        )
                    }

                })
        } else {
            //  Retrieve request id from cache
            val requestId = sharedDefaults.getCachedRequestId()
            NIDLog.d(   
                msg="Retrieving Request ID for Advanced Device Signals from cache: ${requestId}"
            )
            getDataStoreInstance().saveEvent(
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