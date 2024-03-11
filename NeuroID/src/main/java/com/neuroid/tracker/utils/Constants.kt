package com.neuroid.tracker.utils

enum class Constants(val displayName: String) {
    integrationHealthEvents("integrationHealthEvents.json"),
    integrationHealthDevice("integrationHealthDetails.json"),
    integrationHealthFolder("nid"),
    integrationHealthAssetsFolder("integrationHealth"),

    debugEventTag("Event"),
    fpjsProdDomain("https://advanced.neuro-id.com"),
    productionEndpoint("https://receiver.neuroid.cloud/"),
    devEndpoint("https://receiver.neuro-dev.com/"),
} 
