package com.neuroid.tracker.models

import com.google.gson.annotations.SerializedName
import com.neuroid.tracker.service.NIDConfigService

data class NIDLinkedSiteOption(
    @SerializedName(value = "sample_rate")
    val sampleRate: Int = NIDConfigService.DEFAULT_SAMPLE_RATE,
)
