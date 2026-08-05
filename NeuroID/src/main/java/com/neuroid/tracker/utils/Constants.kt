package com.neuroid.tracker.utils

@Suppress("ktlint:standard:enum-entry-name-case")
enum class Constants(
    val displayName: String,
) {
    integrationHealthEvents("integrationHealthEvents.json"),
    integrationHealthDevice("integrationHealthDetails.json"),
    integrationHealthFolder("nid"),
    integrationHealthAssetsFolder("integrationHealth"),

    debugEventTag("Event"),
    devEndpoint("https://receiver.neuro-dev.com/"),
    devScriptsEndpoint("https://scripts.neuro-dev.com/"),
    testScriptEndpoint("http://127.0.0.1:8000/"),
}
