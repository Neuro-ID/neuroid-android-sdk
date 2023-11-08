package com.neuroid.tracker.utils

enum class Constants(val displayName: String) {
    integrationHealthEvents("integrationHealthEvents.json"),
    integrationHealthDevice("integrationHealthDetails.json"),
    integrationHealthFolder("nid"),
    integrationHealthAssetsFolder("integrationHealth"),

    debugEventTag("Event"),
    fpjsProdDomain("http://advanced.neuro-id.com/"),
    fpjsDevDomain("https://advanced.neuro-dev.com")
}