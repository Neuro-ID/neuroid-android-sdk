package com.neuroid.tracker.models

data class ApplicationMetaData(
    val versionName: String,
    val versionNumber: Int,
    val packageName: String,
    val applicationName: String,
) {
    fun toMap(): Map<String, Any>  {
        return mapOf(
            "versionName" to versionName,
            "versionNumber" to versionNumber.toString(),
            "packageName" to packageName,
            "applicationName" to applicationName,
        )
    }
}
