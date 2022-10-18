package com.sample.neuroid.us.domain.network

data class ProfileResponse(
    val status: String,
    val message: String,
    val moreInfo: String?,
    val profile: Profile?
)