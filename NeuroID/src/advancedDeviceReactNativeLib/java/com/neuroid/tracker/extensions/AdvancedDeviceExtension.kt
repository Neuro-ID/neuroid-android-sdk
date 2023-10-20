package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.GsonAdvMapper
import com.neuroid.tracker.utils.HttpConnectionProvider
import com.neuroid.tracker.service.NIDAdvKeyService
import com.neuroid.tracker.service.OnKeyCallback
import com.neuroid.tracker.utils.NIDLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun NeuroID.start(advancedDeviceSignals: Boolean) {
    start()

    if (advancedDeviceSignals) {
        CoroutineScope(Dispatchers.IO).launch {
            val keyService = NIDAdvKeyService()
            var fpjsRetryCount = 0
            val FPJS_RETRY_MAX = 3
            keyService.getKey(object : OnKeyCallback {
                override fun onKeyGotten(key: String) {
                    val applicationContext = getApplicationContext()
                    val fpjsClient = applicationContext?.let {
                        FingerprintJSFactory(applicationContext = it).createInstance(
                            Configuration(
                                apiKey = key
                            )
                        )
                    }
                    while (fpjsRetryCount < FPJS_RETRY_MAX) {
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
                            fpjsRetryCount = FPJS_RETRY_MAX
                        },

                            errorListener = { it ->
                                NIDLog.d(
                                    "NIDDebugEvent",
                                    "Error retrieving Advanced Device Signal Request ID"
                                )
                                fpjsRetryCount += 1
                                Thread.sleep(5000)
                            })
                    }

                }

                override fun onFailure(message: String, responseCode: Int) {
                    NIDLog.e("NeuroId", "cannot get key, message: $message")
                }
            }, HttpConnectionProvider(), GsonAdvMapper(), Base64Decoder(), clientKey)
        }
    }
}