package com.neuroid.tracker.extensions

import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDImpl
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.neuroid.tracker.service.getADVNetworkService
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.Constants

fun NeuroID.start(advancedDeviceSignals: Boolean): Boolean {
    val started = start()

    if (!started) {
        return started
    }

    if (advancedDeviceSignals) {
        NeuroIDImpl.getInternalInstance()?.apply {
            getApplicationContext()?.let { context ->
                getADVSignal(clientKey, context, this, logger)
            }
        }
    }

    return started
}

fun NeuroID.startSession(
    sessionID: String? = null,
    advancedDeviceSignals: Boolean
): SessionStartResult {
    val sessionRes = startSession(
        sessionID
    )

    if (!sessionRes.started) {
        return sessionRes
    }

    if (advancedDeviceSignals) {
        NeuroIDImpl.getInternalInstance()?.apply {
            getApplicationContext()?.let { context ->
                getADVSignal(clientKey, context, this, logger)
            }
        }
    }

    return sessionRes
}

internal fun getADVSignal(
    clientKey: String,
    applicationContext: Context,
    neuroIDImpl: NeuroIDImpl,
    logger: NIDLogWrapper
) {
    CoroutineScope(Dispatchers.IO).launch {
        val advancedDeviceIDManagerService = AdvancedDeviceIDManager(
            applicationContext,
            logger,
            NIDSharedPrefsDefaults(applicationContext),
            neuroIDImpl,
            getADVNetworkService(
                NeuroIDImpl.endpoint,
                logger
            ),
            null
        )

        // check for cachedID first
        if (!advancedDeviceIDManagerService.getCachedID()) {
            // no cached ID - contact NID & FPJS
            advancedDeviceIDManagerService.getRemoteID(
                clientKey,
                Constants.fpjsProdDomain.displayName
            )
        }
    }

}