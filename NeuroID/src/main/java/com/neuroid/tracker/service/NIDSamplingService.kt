package com.neuroid.tracker.service

import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.RandomGenerator

class NIDSamplingService(
    val logger: NIDLogWrapper,
    // we need this to be replaceable for the tests!
    var randomGenerator: RandomGenerator,
    var configService: ConfigService,
) {
    private var isSessionFlowSampled = true

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    fun updateIsSampledStatus(siteID: String?) {
//        ENG-8324 - ignore updating sampling logic
//
//        val currentSampleRate = retrieveSampleRate(siteID)
//        if (currentSampleRate >= MAX_SAMPLE_RATE) {
//            isSessionFlowSampled = true
//            return
//        }
//        val randomValue = randomGenerator.getRandom(MAX_SAMPLE_RATE)
//        if (randomValue < currentSampleRate) {
//            isSessionFlowSampled = true
//            return
//        }
//
//        isSessionFlowSampled = false
    }

    /**
     * Return the sample rate, 100 indicates sample everything, 0 indicates sample nothing!
     */
    private fun retrieveSampleRate(siteID: String?): Int {
        // is the site id passed in a linked site id in the options?, if so return the linked site
        // id sample rate
        if (isLinkedSiteID(siteID)) {
            configService.configCache.linkedSiteOptions[siteID]?.let {
                return it.sampleRate
            }
        }
        // is the site id passed in a parent site id? if so, return the parent site id sample rate.
        if (isCollectionSiteID(siteID)) {
            return configService.configCache.sampleRate
        }

        // else capture all
        return DEFAULT_SAMPLE_RATE
    }

    fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled

    /**
     * is this a linked site id?
     */
    private fun isLinkedSiteID(siteID: String?): Boolean {
        return configService.configCache.linkedSiteOptions.contains(siteID)
    }

    /**
     * is this a parent site id (siteID parameter matches the config parent siteID or is null)?
     */
    private fun isCollectionSiteID(siteID: String?): Boolean {
        if (siteID == null || siteID == configService.configCache.siteID) {
            return true
        }
        return false
    }

    companion object {
        const val MAX_SAMPLE_RATE = 100
        const val DEFAULT_SAMPLE_RATE = 100
    }
}
