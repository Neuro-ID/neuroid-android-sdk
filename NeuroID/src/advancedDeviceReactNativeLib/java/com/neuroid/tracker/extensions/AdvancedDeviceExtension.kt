package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.neuroid.tracker.service.NIDAdvKeyService
import com.neuroid.tracker.service.OnKeyCallback
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.neuroid.tracker.utils.FPJSHelper

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
                    //  Retrieving the Request ID from FPJS
                    var fpjsHelper = FPJSHelper()
                    fpjsHelper.createRequestIdEvent(fpjsClient, fpjsRetryCount, FPJS_RETRY_MAX)
                }

                override fun onFailure(message: String, responseCode: Int) {
                    NIDLog.e("NeuroId", "cannot get key, message: $message")
                }
            }, HttpConnectionProvider(), GsonAdvMapper(), Base64Decoder(), clientKey, getDataStoreInstance())
        }
    }
}