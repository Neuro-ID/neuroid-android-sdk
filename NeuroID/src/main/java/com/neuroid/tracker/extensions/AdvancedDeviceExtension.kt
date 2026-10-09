package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroID.Companion.fpjsClientOverride
import com.neuroid.tracker.NeuroIDPublic
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

@Synchronized
fun NeuroID.captureAdvancedDevice(
    advancedDeviceKey: String?,
    useAdvancedDeviceProxy: Boolean,
    region: NIDRegion = NIDRegion.US_WEST_DEFAULT,
) = runBlocking {
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
