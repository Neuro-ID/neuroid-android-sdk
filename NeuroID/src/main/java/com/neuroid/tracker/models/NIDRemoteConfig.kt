package com.neuroid.tracker.models

import com.google.gson.annotations.SerializedName

data class NIDRemoteConfig(
    @SerializedName(value="call_in_progress")
    val callInProgress: Boolean = true,
    @SerializedName(value="event_queue_flush_interval")
    val eventQueueFlushInterval: Long = 10,
    @SerializedName(value="event_queue_flush_size")
    val eventQueueFlushSize: Int = 2000,
    @SerializedName(value="geo_location")
    val geoLocation: Boolean = true,
    @SerializedName(value="gyro_accel_cadence")
    val gyroAccelCadence: Boolean = false,
    @SerializedName(value="gyro_accel_cadence_time")
    val gyroAccelCadenceTime: Long = 200L,
    @SerializedName(value="request_timeout")
    val requestTimeout: Long = 10) {
}