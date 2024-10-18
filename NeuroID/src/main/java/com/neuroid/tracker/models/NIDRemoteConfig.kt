package com.neuroid.tracker.models

import com.google.gson.annotations.SerializedName
import com.neuroid.tracker.service.NIDConfigService

data class NIDRemoteConfig(
    @SerializedName(value = "call_in_progress")
    val callInProgress: Boolean = true,
    @SerializedName(value = "event_queue_flush_interval")
    val eventQueueFlushInterval: Long = 5,
    @SerializedName(value = "event_queue_flush_size")
    val eventQueueFlushSize: Int = 1999,
    @SerializedName(value = "geo_location")
    val geoLocation: Boolean = false,
    @SerializedName(value = "gyro_accel_cadence")
    val gyroAccelCadence: Boolean = false,
    @SerializedName(value = "gyro_accel_cadence_time")
    val gyroAccelCadenceTime: Long = 200L,
    @SerializedName(value = "request_timeout")
    val requestTimeout: Long = 10,
    @SerializedName(value = "linked_site_options")
    val linkedSiteOptions: HashMap<String, NIDLinkedSiteOption> = hashMapOf(),
    @SerializedName(value = "sample_rate")
    val sampleRate: Int = NIDConfigService.DEFAULT_SAMPLE_RATE,
    @SerializedName(value = "site_id")
    val siteID: String = "",
)
