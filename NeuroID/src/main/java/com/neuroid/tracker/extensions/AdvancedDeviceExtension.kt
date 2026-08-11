package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroID.Companion.fpjsClientOverride
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.NIDRegion
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.service.AdvancedDeviceIDManagerService
import com.neuroid.tracker.service.getADVNetworkService
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Start the SDK and start a new session using the userID as the sessionID. Takes in a boolean to
 * enable/disable the advanced signal collection. Return true to indicate that the SDK is started.
 * Return false if not started.
 */

@Deprecated(
    "Use start(completion) instead",
    ReplaceWith("start(completion)"),
)
fun NeuroIDPublic.start(
    advancedDeviceSignals: Boolean,
    completion: (Boolean) -> Unit = {},
) {
    start {
        completion(it)
    }
}

/**
 * Start a new session. This will start the SDK and start a new session using the session ID
 * that is passed in. If a session ID is not passed in, a session ID at random will be used.
 * Return a session result that contains the session ID and a boolean
 * indicating the started state of the SDK. Takes in a boolean to
 * enable/disable the advanced signal collection.
 */
@Deprecated(
    "Use startSession(sessionID, completion) instead",
    ReplaceWith("startSession(sessionID, completion)"),
)
fun NeuroIDPublic.startSession(
    sessionID: String? = null,
    advancedDeviceSignals: Boolean,
    completion: (SessionStartResult) -> Unit = {},
) {
    startSession(
        sessionID,
    ) {
        completion(it)
    }
}

@Synchronized
fun NeuroID.captureAdvancedDevice(
    advancedDeviceKey: String?,
    useAdvancedDeviceProxy: Boolean,
    region: NIDRegion = NIDRegion.usWest,
) = runBlocking {
    captureEvent(queuedEvent = true, type = LOG, m = "shouldCapture setting: $isAdvancedDevice", level = "INFO")
    NeuroID.getInternalInstance()?.apply {
        getApplicationContext()?.let { context ->
            val advancedDeviceIDManagerService =
                AdvancedDeviceIDManager(
                    context,
                    logger,
                    NIDSharedPrefsDefaults(context),
                    this,
                    getADVNetworkService(
                        region.productionEndpoint,
                        logger,
                    ),
                    this.clientID,
                    this.linkedSiteID ?: "",
                    configService,
                    advancedDeviceKey,
                    fpjsClientOverride,
                    useAdvancedDeviceProxy = useAdvancedDeviceProxy,
                    region = region,
                )
            getADVSignal(advancedDeviceIDManagerService, clientKey, this)?.join()
        }
    }

    // runBlocking returns the value of its last expression, adding an explicit Unit at the end of the runBlocking block anchors the return type to Unit unconditionally
    Unit
}

internal fun getADVSignal(
    advancedDeviceIDManagerService: AdvancedDeviceIDManagerService,
    clientKey: String,
    neuroID: NeuroID,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Job? {
    var job: Job? = null
    // do this in the background off main but wait for it to complete
    if (neuroID.configService.isSessionFlowSampled()) {
        job = CoroutineScope(dispatcher).launch {
            // check for cachedID first
            if (!advancedDeviceIDManagerService.getCachedID()) {
                // no cached ID - contact NID & FPJS
                advancedDeviceIDManagerService.getRemoteID(clientKey)?.join()
            }
        }
    }
    return job
}
