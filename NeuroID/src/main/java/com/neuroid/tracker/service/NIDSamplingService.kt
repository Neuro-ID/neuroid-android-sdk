package com.neuroid.tracker.service

class NIDSamplingService(
    var configService: ConfigService,
) {
    private var isSessionFlowSampled = true

    /**
     * given a site id, tell me if we sample events or not. the site ID will be compared
     */
    fun updateIsSampledStatus(siteID: String?) {
        isSessionFlowSampled = configService.siteIDSampleMap[siteID]?:true
    }

    fun isSessionFlowSampled(): Boolean = this.isSessionFlowSampled
}
