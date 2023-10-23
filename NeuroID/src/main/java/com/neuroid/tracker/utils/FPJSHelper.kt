package com.neuroid.tracker.utils

import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class FPJSHelper {
    fun getRequestId(fpjsClient: FingerprintJS?, fpjsRetryCount: Int, maxRetryCount: Int) {
        var retryCount = fpjsRetryCount
        fpjsClient?.getVisitorId(listener = { result ->
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            NIDLog.d(
                "NIDDebugEvent",
                "Request ID for Advanced Device Signals: ${result.requestId}"
            )
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = ADVANCED_DEVICE_REQUEST,
                    rid = result.requestId,
                    gyro = gyroData,
                    accel = accelData,
                    ts = System.currentTimeMillis(),
                )
            )
        },
            errorListener = { error ->
                retryCount += 1
                NIDLog.d(
                    "NIDDebugEvent",
                    "Error retrieving Advanced Device Signal Request ID:${error.description}: $fpjsRetryCount"
                )
                Thread.sleep(5000)
                if (retryCount < maxRetryCount) {
                    getRequestId(fpjsClient, retryCount, maxRetryCount)
                } else {
                    NIDLog.d(
                        "NIDDebugEvent",
                        "Reached maximum number of retries $maxRetryCount to get Advanced Device Signal Request ID"
                    )
                }

            })
    }
}