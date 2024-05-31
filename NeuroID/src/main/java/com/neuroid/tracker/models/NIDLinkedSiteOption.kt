package com.neuroid.tracker.models

import com.google.gson.annotations.SerializedName

data class NIDLinkedSiteOption(
    @SerializedName(value = "sample_rate")
    val sampleRate: Int = 100,
)
