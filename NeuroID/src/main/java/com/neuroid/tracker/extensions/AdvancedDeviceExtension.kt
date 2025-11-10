package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.SessionStartResult
import com.neuroid.tracker.service.AdvancedDeviceIDManager
import com.neuroid.tracker.service.AdvancedDeviceIDManagerService
import com.neuroid.tracker.service.getADVNetworkService
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.Constants
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
fun NeuroIDPublic.start(
    advancedDeviceSignals: Boolean,
    completion: (Boolean) -> Unit = {},
) {
    start {
        if (!it) {
            completion(it)
        } else {
            NeuroID.getInternalInstance()?.checkThenCaptureAdvancedDevice(
                shouldCapture = advancedDeviceSignals)

            completion(it)
        }
    }
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
    completion: (SessionStartResult) -> Unit = {},
) {
    startSession(
        sessionID,
    ) {
        if (!it.started) {
            completion(it)
        } else {
            NeuroID.getInternalInstance()?.checkThenCaptureAdvancedDevice(
                shouldCapture = advancedDeviceSignals)

            completion(it)
        }
    }
}

@Synchronized
fun NeuroID.captureAdvancedDevice(shouldCapture: Boolean, advancedDeviceKey: String?,
                                  useAdvancedDeviceProxy: Boolean) = runBlocking {
    captureEvent(queuedEvent = true, type = LOG, m = "shouldCapture setting: $shouldCapture", level = "INFO")
    if (shouldCapture) {
        NeuroID.getInternalInstance()?.apply {
            getApplicationContext()?.let { context ->
                val advancedDeviceIDManagerService =
                    AdvancedDeviceIDManager(
                        context,
                        logger,
                        NIDSharedPrefsDefaults(context),
                        this,
                        getADVNetworkService(
                            NeuroID.endpoint,
                            logger
                        ),
                        this.clientID,
                        this.linkedSiteID ?: "",
                        configService,
                        advancedDeviceKey,
                        useAdvancedDeviceProxy = useAdvancedDeviceProxy
                    )
                getADVSignal(advancedDeviceIDManagerService, clientKey, this)?.join()
            }
        }
    } else {
        logger.d(msg = "in captureAdvancedDevice(), advanced device not active.")
    }
}

internal fun getADVSignal(
    advancedDeviceIDManagerService: AdvancedDeviceIDManagerService,
    clientKey: String,
    neuroID: NeuroID,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Job? {
    var job: Job? = null
    // do this in the background off main but wait for it to complete
    if (neuroID.configService.isSessionFlowSampled()) {
        job = CoroutineScope(dispatcher).launch {
            // check for cachedID first
            if (!advancedDeviceIDManagerService.getCachedID()) {
                // no cached ID - contact NID & FPJS
                advancedDeviceIDManagerService.getRemoteID(
                    clientKey
                )?.join()
            }
        }
    }
    return job
}
