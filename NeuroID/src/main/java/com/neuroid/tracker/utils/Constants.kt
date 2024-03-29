package com.neuroid.tracker.utils

enum class Constants(val displayName: String) {
    integrationHealthEvents("integrationHealthEvents.json"),
    integrationHealthDevice("integrationHealthDetails.json"),
    integrationHealthFolder("nid"),
    integrationHealthAssetsFolder("integrationHealth"),

    debugEventTag("Event"),
    fpjsProdDomain("https://advanced.neuro-id.com"),
    productionEndpoint("https://receiver.neuroid.cloud/"),
    productionScriptsEndpoint("http://scripts.neuro-id.com/"),
    devEndpoint("https://receiver.neuro-dev.com/"),
    devScriptsEndpoint("http://scripts.neuro-dev.com/")
} 
