package com.neuroid.tracker.extensions
import com.neuroid.tracker.NeuroID
import com.fingerprintjs.android.fpjs_pro.Configuration
import com.fingerprintjs.android.fpjs_pro.FingerprintJSFactory

fun NeuroID.start(advancedDeviceSignals: Boolean) {
    start()

    if (advancedDeviceSignals) {
        val applicationContext = getApplicationContext()
        val fpjsClient = applicationContext?.let {
            FingerprintJSFactory(applicationContext = it).createInstance(
                Configuration(
                    apiKey = "ADD_KEY"
                )
            )
        }

        fpjsClient?.getVisitorId(listener = { result ->
            println("XXXXXX: ${result.visitorId}")
//            send ID to BE
        },

            errorListener = { it ->
                println(it.description)
            })
    }
}