package com.neuroid.tracker.models

data class ApplicationMetaData(
    val versionName: String,
    val versionNumber: Int,
    val packageName: String,
    val applicationName: String,
    val rnVersion: String = "",
    val minOSVersion: Int = -1,
) {
    fun toList(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "n" to "rnVersion",
                "v" to rnVersion
            ),
            mapOf(
                "n" to "minOSVersion",
                "v" to minOSVersion
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
