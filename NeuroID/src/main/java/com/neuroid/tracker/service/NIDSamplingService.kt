package com.neuroid.tracker.service

import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.RandomGenerator

class NIDSamplingService(
    val logger: NIDLogWrapper,
    // we need this to be replaceable for the tests!
    var randomGenerator: RandomGenerator,
    var configService: NIDRemoteConfigService,
) {
    private var isSessionFlowSampled = true

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    fun updateIsSampledStatus(siteID: String?) {
        val currentSampleRate = retrieveSampleRate(siteID)
        if (currentSampleRate >= MAX_SAMPLE_RATE) {
            logger.d(msg="updateIsSampledStatus() we are sampling at max or > than max ($currentSampleRate)")
            isSessionFlowSampled = true
            return
        }
        val randomValue = randomGenerator.getRandom(MAX_SAMPLE_RATE)
        logger.d(msg="updateIsSampledStatus() random value: $randomValue sampleRate: $currentSampleRate")
        if (randomValue < currentSampleRate) {
            isSessionFlowSampled = true
            logger.d(msg="updateIsSampledStatus() we are sampling")
            return
        }

        logger.d(msg="updateIsSampledStatus() we are not sampling")
        isSessionFlowSampled = false
    }


    /**
     * Return the sample rate, 0 indicates sample everything, 100 indicates sample nothing!
     */
    private fun retrieveSampleRate(siteID: String?): Int {
        // is the site id passed in a linked site id in the options?, if so return the linked site
        // id sample rate
        if (isLinkedSiteID(siteID)) {
            configService.getRemoteNIDConfig().linkedSiteOptions[siteID]?.let {
                logger.d(msg = "retrieveSampleRate site id is in options, ${it.sampleRate}")
                return it.sampleRate
            }
        }
        // is the site id passed in a parent site id? if so, return the parent site id sample rate.
        if (isCollectionSiteID(siteID)) {
            logger.d(msg = "retrieveSampleRate parent site, ${configService.getRemoteNIDConfig().sampleRate}")
            return configService.getRemoteNIDConfig().sampleRate
        }

        // else capture all
        logger.d(msg="retrieveSampleRate not in options and not parent, capture all, 0")
        return DEFAULT_SAMPLE_RATE
    }

    fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled

    /**
     * is this a linked site id?
     */
    private fun isLinkedSiteID(siteID: String?): Boolean {
        return configService.getRemoteNIDConfig().linkedSiteOptions.contains(siteID)
    }


    /**
     * is this a parent site id (siteID parameter matches the config parent siteID or is null)?
     */
    private fun isCollectionSiteID(siteID: String?): Boolean {
        if (siteID == null || siteID == configService.getRemoteNIDConfig().siteID) {
            return true
        }
        return false
    }


    companion object {
        const val MAX_SAMPLE_RATE = 100
        const val DEFAULT_SAMPLE_RATE = 100
    }
}
