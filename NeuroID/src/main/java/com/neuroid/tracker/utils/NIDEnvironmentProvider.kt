package com.neuroid.tracker.utils

interface NIDEnvironmentProvider {
    fun getenv(name: String): String?
    fun getProperty(key: String): String?
}

class NIDSystemEnvironmentProvider : NIDEnvironmentProvider {
    override fun getenv(name: String): String? = System.getenv(name)
    override fun getProperty(key: String): String? = System.getProperty(key)
}