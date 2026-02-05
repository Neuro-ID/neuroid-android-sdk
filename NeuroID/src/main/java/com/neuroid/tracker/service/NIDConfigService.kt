package com.neuroid.tracker.service

import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CLEAR_SAMPLE_SITE_ID_MAP
import com.neuroid.tracker.events.CONFIG_CACHED
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.UPDATE_SAMPLE_SITE_ID_MAP
import com.neuroid.tracker.events.UPDATE_IS_SAMPLED_STATUS
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDResponseCallBack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface ConfigService {
    val configCache: NIDRemoteConfig
    val siteIDSampleMap: MutableMap<String, Boolean>

    fun retrieveOrRefreshCache(neuroID: NeuroID,
                               dispatcher: CoroutineDispatcher = Dispatchers.IO,
                               httpService: HttpService = neuroID.httpService,
                               gson: Gson = Gson(),
                               configRetrievalCallback: () -> Unit = {})
    fun clearSiteIDSampleMap(neuroID: NeuroID)
    fun isSessionFlowSampled(): Boolean
    fun updateIsSampledStatus(neuroID: NeuroID, siteID: String?)
}

internal class NIDConfigService: ConfigService {
    companion object {
        const val DEFAULT_SAMPLE_RATE: Int = 100
        const val MAX_SAMPLE_RATE = 100
    }

    var cacheSetWithRemote = false
    var cacheCreationTime: Long = 0L
    private var isSessionFlowSampled = true

    override var configCache: NIDRemoteConfig = NIDRemoteConfig()
    override var siteIDSampleMap: MutableMap<String, Boolean> = mutableMapOf()

    internal fun retrieveConfig(dispatcher: CoroutineDispatcher,
                                neuroID: NeuroID,
                                httpService: HttpService = neuroID.httpService,
                                gson: Gson,
                                completion: () -> Unit = {}) {
        if (!neuroID.validationService.verifyClientKeyExists(neuroID.clientKey)) {
            cacheSetWithRemote = false
            completion()
            return
        }

        CoroutineScope(dispatcher).launch {
            retrieveConfigCoroutine( neuroID, httpService, gson, completion)
        }
    }

    /***
     * This function is broke out from `retrieveConfig` in order to test because our coroutine
     * tests immediately end and do not run the inner function
     */
    internal fun retrieveConfigCoroutine(neuroID: NeuroID,
                                         httpService: HttpService = neuroID.httpService,
                                         gson: Gson,
                                         completion: () -> Unit) {
        httpService.getConfig(
            neuroID.clientKey,
            object : NIDResponseCallBack<NIDRemoteConfig> {
                override fun onSuccess(
                    code: Int,
                    response: NIDRemoteConfig,
                ) {
                    setCache(response)
                    neuroID.logger.i(msg = "NID remoteConfig: $response")

                    cacheSetWithRemote = true
                    cacheCreationTime = neuroID.nidTime.getCurrentTimeMillis()
                    initSiteIDSampleMap(neuroID, response)
                    captureConfigEvent(gson, neuroID, response)
                    completion()
                }

                override fun onFailure(
                    code: Int,
                    message: String,
                    isRetry: Boolean,
                ) {
                    neuroID.captureEvent(
                        type = LOG,
                        m =
                            """
                             Failed to retrieve NID Config for key ${neuroID.clientKey}. Default values will be used
                            """.replace("\n", "").trim(),
                        level = "ERROR",
                    )
                    neuroID.logger.e(
                        msg =
                            """
                             Failed to retrieve NID Config for key ${neuroID.clientKey}. Default values will be used
                            """.replace("\n", "").trim(),
                    )

                    val newConfig = NIDRemoteConfig()
                    setCache(newConfig)
                    captureConfigEvent(gson, neuroID, newConfig)
                    cacheSetWithRemote = false
                    completion()
                }
            },
        )
    }

    override fun clearSiteIDSampleMap(neuroID: NeuroID) {
        siteIDSampleMap.clear()
        neuroID.captureEvent(
            queuedEvent = true,
            type = CLEAR_SAMPLE_SITE_ID_MAP
        )
    }

    internal fun initSiteIDSampleMap(neuroID: NeuroID,
                                     config: NIDRemoteConfig) {
        for (linkedSiteID in config.linkedSiteOptions.keys) {
            config.linkedSiteOptions[linkedSiteID]?.let {
                if (it.sampleRate == 0) {
                    siteIDSampleMap[linkedSiteID] = false
                } else {
                    siteIDSampleMap[linkedSiteID] =
                        neuroID.randomGenerator.getRandom(MAX_SAMPLE_RATE) <= it.sampleRate
                }
            }
        }

        if (config.sampleRate == 0) {
            siteIDSampleMap[config.siteID] = false
        } else {
            siteIDSampleMap[config.siteID] =
                neuroID.randomGenerator.getRandom(MAX_SAMPLE_RATE) <= config.sampleRate
        }

        neuroID.captureEvent(
            queuedEvent = true,
            type = UPDATE_SAMPLE_SITE_ID_MAP
        )
    }

    fun setCache(newCache: NIDRemoteConfig) {
        configCache = newCache
    }

    internal fun expiredCache(): Boolean {
        return !cacheSetWithRemote
    }

    override fun retrieveOrRefreshCache(neuroID: NeuroID,
                                        dispatcher: CoroutineDispatcher,
                                        httpService: HttpService,
                                        gson: Gson,
                                        configRetrievalCallback: () -> Unit) {
        if (expiredCache()) {
            retrieveConfig(dispatcher, neuroID, httpService, gson, configRetrievalCallback)
        }
    }

    fun captureConfigEvent(gson: Gson, neuroID: NeuroID, configData: NIDRemoteConfig) {
        try {
            neuroID.captureEvent(
                type = CONFIG_CACHED,
                v = gson.toJson(configData),
            )
        } catch (e: Exception) {
            neuroID.captureEvent(
                type = LOG,
                m = "Failed to parse config",
                level = "ERROR",
            )
        }
    }

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    override fun updateIsSampledStatus(neuroID: NeuroID, siteID: String?) {
        isSessionFlowSampled = siteIDSampleMap[siteID]?:true
        neuroID.captureEvent(
            queuedEvent = true,
            type = UPDATE_IS_SAMPLED_STATUS,
        )
    }

    /**
     * Returns whether the session flow is sampled based on the current configuration.
     */
    override fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled
}
