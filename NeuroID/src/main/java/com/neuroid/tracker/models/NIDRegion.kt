package com.neuroid.tracker.models

@Suppress("ktlint:standard:enum-entry-name-case")
enum class NIDRegion(
    val fpjsProdDomain: String,
    val fpjsPrimaryDomain: String,
    val productionEndpoint: String,
    val productionScriptsEndpoint: String,
) {
    US_WEST_DEFAULT(
        fpjsProdDomain = "https://advanced.neuro-id.com",
        fpjsPrimaryDomain = "https://dn.neuroid.cloud/iynlfqcb0t",
        productionEndpoint = "https://receiver.neuroid.cloud/",
        productionScriptsEndpoint = "https://scripts.neuro-id.com/",
    ),

    @Deprecated("Use US_WEST_DEFAULT instead", ReplaceWith("NIDRegion.US_WEST_DEFAULT"))
    usWest(
        fpjsProdDomain = "https://advanced.neuro-id.com",
        fpjsPrimaryDomain = "https://dn.neuroid.cloud/iynlfqcb0t",
        productionEndpoint = "https://receiver.neuroid.cloud/",
        productionScriptsEndpoint = "https://scripts.neuro-id.com/",
    ),
    US_EAST(
        fpjsProdDomain = "https://advanced.neuro-id.com",
        fpjsPrimaryDomain = "https://dn.neuroid.cloud/use2/iynlfqcb0t",
        productionEndpoint = "https://edge.neuroid.cloud/use2/",
        productionScriptsEndpoint = "https://scripts.neuro-id.com/",
    ),
    US_WEST(
        fpjsProdDomain = "https://advanced.neuro-id.com",
        fpjsPrimaryDomain = "https://dn.neuroid.cloud/usw2/iynlfqcb0t",
        productionEndpoint = "https://edge.neuroid.cloud/usw2/",
        productionScriptsEndpoint = "https://scripts.neuro-id.com/",
    ),
    TEST(
        fpjsProdDomain = "http://127.0.0.1:8000",
        fpjsPrimaryDomain = "http://127.0.0.1:8000",
        productionEndpoint = "http://127.0.0.1:8000/",
        productionScriptsEndpoint = "http://127.0.0.1:8000/",
    ),
}
