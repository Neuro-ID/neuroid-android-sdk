package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.GsonAdvMapper
import com.neuroid.tracker.utils.HttpConnectionProvider
import com.neuroid.tracker.service.NIDAdvKeyService
import com.neuroid.tracker.service.OnKeyCallback
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.neuroid.tracker.utils.FPJSHelper
import com.neuroid.tracker.utils.NIDLogWrapper

fun NeuroID.start(advancedDeviceSignals: Boolean) {
    start()
    if (advancedDeviceSignals) {
        // Retrieving the API key from NID
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
                    applicationContext?.let {
                        FPJSHelper().createRequestIdEvent(fpjsClient, fpjsRetryCount, FPJS_RETRY_MAX,
                            NIDSharedPrefsDefaults(it), NIDLogWrapper(), getDataStoreInstance())
                    }
                }

                override fun onFailure(message: String, responseCode: Int) {
                    // do some error handling with the error
                    NIDLog.e("NeuroId", "Failed to get API key from NID: $message")
                }
            }, HttpConnectionProvider(), GsonAdvMapper(), Base64Decoder(), clientKey,
                getDataStoreInstance())
        }
    }
}

