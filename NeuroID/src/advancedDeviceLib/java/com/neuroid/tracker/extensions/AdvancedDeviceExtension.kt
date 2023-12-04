package com.neuroid.tracker.extensions

import android.content.Context
import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.GsonAdvMapper
import com.neuroid.tracker.utils.HttpConnectionProvider
import com.neuroid.tracker.service.NIDAdvKeyService
import com.neuroid.tracker.service.OnKeyCallback
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.neuroid.tracker.utils.FPJSHelper
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.Constants

fun NeuroID.start(advancedDeviceSignals: Boolean): Boolean {
    val started = start()

    if (!started) {
        return started
    }

    if (advancedDeviceSignals) {
        getADVSignal(clientKey, getApplicationContext(), NIDLog)
    }

    return started
}

fun NeuroID.startSession(
    sessionID: String = "",
    advancedDeviceSignals: Boolean
): SessionStartResult {
    val sessionRes = startSession(
        sessionID
    )

    if (!sessionRes.started) {
        return sessionRes
    }

    if (advancedDeviceSignals) {
        getADVSignal(clientKey, getApplicationContext(), NIDLog)
    }

    return sessionRes
}

internal fun getADVSignal(
    clientKey: String,
    applicationContext: Context?,
    NIDLog: NIDLogWrapper
) {

    // Retrieving the API key from NeuroID
    CoroutineScope(Dispatchers.IO).launch {
        val keyService = NIDAdvKeyService()
        var fpjsRetryCount = 0
        val FPJS_RETRY_MAX = 3
        var endpointUrl = Constants.fpjsProdDomain.displayName

        keyService.getKey(
            object : OnKeyCallback {
                override fun onKeyGotten(key: String) {
                    val fpjsClient = applicationContext?.let {
                        FingerprintJSFactory(applicationContext = it).createInstance(
                            Configuration(
                                apiKey = key,
                                endpointUrl = endpointUrl
                            )
                        )
                    }
                    //  Retrieving the Request ID from FPJS
                    applicationContext?.let {
                        FPJSHelper().createRequestIdEvent(
                            fpjsClient,
                            fpjsRetryCount,
                            FPJS_RETRY_MAX,
                            NIDSharedPrefsDefaults(it),
                            NIDLogWrapper(),
                            getDataStoreInstance()
                        )
                    }
                }

                override fun onFailure(message: String, responseCode: Int) {
                    // do some error handling with the error
                    NIDLog.e(msg = "Failed to get API key from NeuroID: $message")
                }
            },
            HttpConnectionProvider(),
            GsonAdvMapper(),
            Base64Decoder(),
            clientKey,
            getDataStoreInstance()
        )
    }

}