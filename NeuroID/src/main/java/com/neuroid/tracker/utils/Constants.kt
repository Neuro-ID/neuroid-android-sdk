package com.neuroid.tracker.utils

enum class Constants(val displayName: String) {
    integrationHealthEvents("integrationHealthEvents.json"),
    integrationHealthDevice("integrationHealthDetails.json"),
    integrationHealthFolder("nid"),
    integrationHealthAssetsFolder("integrationHealth"),

    debugEventTag("Event"),
    fpjsProdDomain("https://advanced.neuro-id.com"),
    productionEndpoint("https://receiver.neuroid.cloud/"),
    productionScriptsEndpoint("https://scripts.neuro-id.com/"),
    devEndpoint("https://receiver.neuro-dev.com/"),
    devScriptsEndpoint("https://scripts.neuro-dev.com/"),
    testScriptEndpoint("http://127.0.0.1:8000/")

}
