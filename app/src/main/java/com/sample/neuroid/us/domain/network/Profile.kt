package com.sample.neuroid.us.domain.network

data class Profile(
    val id: String,
    val siteId: String,
    val funnel: String,
    val clientId: String,
    val interactionAttributes: String? = "",
    val signals: List<Signal>,
)