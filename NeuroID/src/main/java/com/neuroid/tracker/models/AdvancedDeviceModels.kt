package com.neuroid.tracker.models

data class ADVKeyNetworkResponse(var status: String, var key: String)

data class ADVKeyFunctionResponse(val key: String, val success: Boolean, val message: String? = null)
