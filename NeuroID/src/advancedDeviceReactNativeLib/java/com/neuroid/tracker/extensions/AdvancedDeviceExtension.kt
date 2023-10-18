package com.neuroid.tracker.extensions
import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.events.FPJS_REQUEST
import com.neuroid.tracker.utils.NIDLog

fun NeuroID.start(advancedDeviceSignals: Boolean) {
    start()

    if (advancedDeviceSignals) {
        val applicationContext = getApplicationContext()
        val fpjsClient = applicationContext?.let {
            FingerprintJSFactory(applicationContext = it).createInstance(
                Configuration(
                    apiKey = "API-KEY"
                )
            )
        }

        fpjsClient?.getVisitorId(listener = { result ->
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance().saveEvent(NIDEventModel(
                type = FPJS_REQUEST,
                rid = result.visitorId,
                gyro = gyroData,
                accel = accelData,
                ts = System.currentTimeMillis(),
            ))
        },

            errorListener = { it ->
                NIDLog.d("NIDDebugEvent","Error retrieving Advanced Device Signal ID")
            })
    }
}