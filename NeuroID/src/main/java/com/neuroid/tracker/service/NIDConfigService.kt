package com.neuroid.tracker.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CLEAR_SAMPLE_SITE_ID_MAP
import com.neuroid.tracker.events.CONFIG_CACHED
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.UPDATE_SAMPLE_SITE_ID_MAP
import com.neuroid.tracker.events.UPDATE_IS_SAMPLED_STATUS
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTime
import com.neuroid.tracker.utils.RandomGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

interface ConfigService {
    val configCache: NIDRemoteConfig
    val siteIDSampleMap: MutableMap<String, Boolean>

    fun retrieveOrRefreshCache()
    fun clearSiteIDSampleMap()
    fun isSessionFlowSampled(): Boolean
    fun updateIsSampledStatus(siteID: String?)
}

internal class NIDConfigService(
    private val dispatcher: CoroutineDispatcher,
    private val logger: NIDLogWrapper,
    private val neuroID: NeuroID,
    private val httpService: HttpService,
    private val validationService: NIDValidationService,
    private val gson: Gson = GsonBuilder().create(),
    private val randomGenerator: RandomGenerator = RandomGenerator(),
    private val configRetrievalCallback: () -> Unit = {},
    val nidTime: NIDTime = NIDTime()
) : ConfigService {
    companion object {
        const val DEFAULT_SAMPLE_RATE: Int = 100
        const val MAX_SAMPLE_RATE = 100
    }

    var cacheSetWithRemote = false
    var cacheCreationTime: Long = 0L
    private var isSessionFlowSampled = true

    override var configCache: NIDRemoteConfig = NIDRemoteConfig()
    override var siteIDSampleMap: MutableMap<String, Boolean> = mutableMapOf()

    internal fun retrieveConfig() {
        if (!validationService.verifyClientKeyExists(neuroID.clientKey)) {
            cacheSetWithRemote = false
            configRetrievalCallback()
            return
        }

        CoroutineScope(dispatcher).launch {
            retrieveConfigCoroutine {
                configRetrievalCallback()
            }
        }
    }

    /***
     * This function is broke out from `retrieveConfig` in order to test because our coroutine
     * tests immediately end and do not run the inner function
     */
    internal fun retrieveConfigCoroutine(completion: () -> Unit) {
        httpService.getConfig(
            neuroID.clientKey,
            object : NIDResponseCallBack<NIDRemoteConfig> {
                override fun onSuccess(
                    code: Int,
                    response: NIDRemoteConfig,
                ) {
                    setCache(response)
                    logger.i(msg = "NID remoteConfig: $response")

                    cacheSetWithRemote = true
                    cacheCreationTime = nidTime.getCurrentTimeMillis()
                    initSiteIDSampleMap(response)
                    captureConfigEvent(response)
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
                    logger.e(
                        msg =
                            """
                             Failed to retrieve NID Config for key ${neuroID.clientKey}. Default values will be used
                            """.replace("\n", "").trim(),
                    )

                    val newConfig = NIDRemoteConfig()
                    setCache(newConfig)
                    captureConfigEvent(newConfig)
                    cacheSetWithRemote = false
                    completion()
                }
            },
        )
    }

    override fun clearSiteIDSampleMap() {
        siteIDSampleMap.clear()
        neuroID.captureEvent(
            queuedEvent = true,
            type = CLEAR_SAMPLE_SITE_ID_MAP
        )
    }

    internal fun initSiteIDSampleMap(config: NIDRemoteConfig) {
        for (linkedSiteID in config.linkedSiteOptions.keys) {
            config.linkedSiteOptions[linkedSiteID]?.let {
                if (it.sampleRate == 0) {
                    siteIDSampleMap[linkedSiteID] = false
                } else {
                    siteIDSampleMap[linkedSiteID] =
                        randomGenerator.getRandom(MAX_SAMPLE_RATE) <= it.sampleRate
                }
            }
        }

        if (config.sampleRate == 0) {
            siteIDSampleMap[config.siteID] = false
        } else {
            siteIDSampleMap[config.siteID] =
                randomGenerator.getRandom(MAX_SAMPLE_RATE) <= config.sampleRate
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

    override fun retrieveOrRefreshCache() {
        if (expiredCache()) {
            retrieveConfig()
        }
    }

    fun captureConfigEvent(configData: NIDRemoteConfig) {
        try {
            this.neuroID.captureEvent(
                type = CONFIG_CACHED,
                v = gson.toJson(configData),
            )
        } catch (e: Exception) {
            this.neuroID.captureEvent(
                type = LOG,
                m = "Failed to parse config",
                level = "ERROR",
            )
        }
    }

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    override fun updateIsSampledStatus(siteID: String?) {
        isSessionFlowSampled = siteIDSampleMap[siteID]?:true
        this.neuroID.captureEvent(
            queuedEvent = true,
            type = UPDATE_IS_SAMPLED_STATUS,
        )
    }

    /**
     * Returns whether the session flow is sampled based on the current configuration.
     */
    override fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled
}
