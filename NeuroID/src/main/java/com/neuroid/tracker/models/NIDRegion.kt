package com.neuroid.tracker.models

enum class NIDRegion(
    val fpjsProdDomain: String,
    val fpjsPrimaryDomain: String,
    val productionEndpoint: String,
    val productionScriptsEndpoint: String) {

    usWest(
        fpjsProdDomain = "https://advanced.neuro-id.com",
        fpjsPrimaryDomain = "https://dn.neuroid.cloud/iynlfqcb0t",
        productionEndpoint = "https://receiver.neuroid.cloud/",
        productionScriptsEndpoint = "https://scripts.neuro-id.com/",
    ),
}
