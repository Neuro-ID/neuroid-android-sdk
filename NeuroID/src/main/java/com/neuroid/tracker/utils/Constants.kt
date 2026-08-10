package com.neuroid.tracker.utils

@Suppress("ktlint:standard:enum-entry-name-case")
enum class Constants(
    val displayName: String,
) {
    devEndpoint("https://receiver.neuro-dev.com/"),
    devScriptsEndpoint("https://scripts.neuro-dev.com/"),
}
