package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.RandomGenerator
import java.util.Calendar

class NIDSamplingService(
    val logger: NIDLogWrapper,
    // we need this to be replaceable for the tests!
    var randomGenerator: RandomGenerator
) {

    private var isSessionFlowSampled = true
    private var lastUpdatedTime = 0L

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    fun updateIsSampledStatus(siteID: String?) {
        val currentSampleRate = retrieveSampleRate(siteID)
        if (currentSampleRate >= MAX_SAMPLE_RATE) {
            logger.d(msg="updateIsSampledStatus() we are sampling default")
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
        // let updateConfig() figure out if we need to update or not
        logger.d(msg="update config")
        updateConfig()
        logger.d(msg="config updated")

        // is the site id passed in a linked site id in the options?, if so return the linked site
        // id sample rate
        if (isLinkedSiteID(siteID)) {
            NeuroID.nidSDKConfig.linkedSiteOptions[siteID]?.let {
                logger.d(msg="retrieveSampleRate site id is in options, ${it.sampleRate}")
                return it.sampleRate
            }
        }
        // is the site id passed in a parent site id? if so, return the parent site id sample rate.
        if (isCollectionSiteID(siteID)) {
            logger.d(msg="retrieveSampleRate parent site, ${NeuroID.nidSDKConfig.sampleRate}")
            return NeuroID.nidSDKConfig.sampleRate
        }

        // else capture all
        logger.d(msg="retrieveSampleRate not in options and not parent, capture all, 0")
        return DEFAULT_SAMPLE_RATE
    }

    fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled

    /**
     * is this a linked site id?
     */
    private fun isLinkedSiteID(siteID: String?): Boolean =
        NeuroID.nidSDKConfig.linkedSiteOptions.contains(siteID)

    /**
     * is this a parent site id?
     */
    private fun isCollectionSiteID(siteID: String?): Boolean =
        NeuroID.nidSDKConfig.siteID == (siteID ?: false)

    /**
     * update the config if older than 5 minutes
     */
    private fun updateConfig() {
        // figure out if we need to update the config, if last update was over 5 minutes ago
        // get a new config and replace it, this should block till done.
        val currentTime = Calendar.getInstance().timeInMillis
        if ((currentTime - lastUpdatedTime) > (UPDATE_FREQUENCY)) {
            logger.d(msg="refresh required: ${currentTime - lastUpdatedTime}" )
            NeuroID.getInternalInstance()?.setRemoteConfig()
            lastUpdatedTime = currentTime
        }
    }

    companion object {
        const val UPDATE_FREQUENCY = 60000 * 5 // 5 minutes
        const val MAX_SAMPLE_RATE = 100
        const val DEFAULT_SAMPLE_RATE = 100
    }
}
