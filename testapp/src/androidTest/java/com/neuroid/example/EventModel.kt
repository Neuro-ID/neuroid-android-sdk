package com.neuroid.example

data class EventModel(val linkedSiteId: String = "",
                      val siteId: String = "",
                      val clientId: String = "",
                      val jsonEvents: List<JsonEventModel>)

data class JsonEventModel(val ts: String = "",
                          val type: String = "")