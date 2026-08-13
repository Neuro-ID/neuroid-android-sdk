package com.neuroid.example

import com.neuroid.tracker.models.NIDEventModel

data class EventModel(val linkedSiteId: String = "",
                      val siteId: String = "",
                      val clientId: String = "",
                      val identityId: String? = null,
                      val jsonEvents: List<NIDEventModel>)
