package com.neuroid.tracker.extensions

import android.content.Context
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.service.getADVNetworkService
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Start the SDK and start a new session using the userID as the sessionID. Takes in a boolean to
 * enable/disable the advanced signal collection. Return true to indicate that the SDK is started.
 * Return false if not started.
 */
fun NeuroIDPublic.start(advancedDeviceSignals: Boolean): Boolean {
    val started = start()

    if (!started) {
        return started
    }

    if (advancedDeviceSignals) {
        NeuroID.getInternalInstance()?.apply {
            getApplicationContext()?.let { context ->
                getADVSignal(clientKey, context, this, logger)
            }
        }
    }

    return started
}

/**
 * Start a new session. This will start the SDK and start a new session using the session ID
 * that is passed in. If a session ID is not passed in, a session ID at random will be used.
 * Return a session result that contains the session ID and a boolean
 * indicating the started state of the SDK. Takes in a boolean to
 * enable/disable the advanced signal collection.
 */
fun NeuroIDPublic.startSession(
    sessionID: String? = null,
    advancedDeviceSignals: Boolean,
): SessionStartResult {
    val sessionRes =
        startSession(
            sessionID,
        )

    if (!sessionRes.started) {
        return sessionRes
    }

    if (advancedDeviceSignals) {
        NeuroID.getInternalInstance()?.apply {
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
    neuroID: NeuroID,
    logger: NIDLogWrapper,
) {
    CoroutineScope(Dispatchers.IO).launch {
        val advancedDeviceIDManagerService =
            AdvancedDeviceIDManager(
                applicationContext,
                logger,
                NIDSharedPrefsDefaults(applicationContext),
                neuroID,
                getADVNetworkService(
                    NeuroID.endpoint,
                    logger,
                ),
                null,
            )

        // check for cachedID first
        if (!advancedDeviceIDManagerService.getCachedID()) {
            // no cached ID - contact NID & FPJS
            advancedDeviceIDManagerService.getRemoteID(
                clientKey,
                Constants.fpjsProdDomain.displayName,
            )
        }
    }
}
