package com.neuroid.tracker.utils

interface NIDEnvironmentProvider {
    fun getenv(name: String): String?
    fun getProperty(key: String): String?
}

class NIDSystemEnvironmentProvider : NIDEnvironmentProvider {
    override fun getenv(name: String): String? = System.getenv(name)
    override fun getProperty(key: String): String? = System.getProperty(key)
}

class NIDTestEnvironmentProvider(
    private val envVars: Map<String, String> = emptyMap(),
    private val properties: Map<String, String> = emptyMap()
) : NIDEnvironmentProvider {
    override fun getenv(name: String): String? = envVars[name]
    override fun getProperty(key: String): String? = properties[key]
}