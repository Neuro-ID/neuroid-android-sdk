package com.neuroid.tracker.models

import java.util.logging.Level

data class ApplicationMetaData(
    val versionName: String,
    val versionNumber: Int,
    val packageName: String,
    val applicationName: String,
    val hostRNVersion: String = "",
    val hostMinSDKLevel: Int = -1,
) {
    fun toList(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "n" to "hostRNVersion",
                "v" to hostRNVersion
            ),
            mapOf(
                "n" to "hostMinSDKLevel",
                "v" to hostMinSDKLevel
            ),
            mapOf(
                "n" to "versionName",
                "v" to versionName,
            ),
            mapOf(
                "n" to "versionNumber",
                "v" to versionNumber,
            ),
            mapOf(
                "n" to "packageName",
                "v" to packageName,
            ),
            mapOf(
                "n" to "applicationName",
                "v" to applicationName,
            ),
        )
    }
}
