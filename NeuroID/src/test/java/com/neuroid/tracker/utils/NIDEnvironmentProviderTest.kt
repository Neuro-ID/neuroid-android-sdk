package com.neuroid.tracker.utils

import org.junit.Test

class NIDEnvironmentProviderTest {
    @Test
    fun testGetEnv() {
        val envProvider = NIDSystemEnvironmentProvider()
        val javaHome = envProvider.getProperty("java.home")
        assert(!javaHome.isNullOrEmpty())
    }

    @Test
    fun getEnvProperties() {
        val envProvider = NIDSystemEnvironmentProvider()
        val javaHome = envProvider.getProperty("java.home")
        assert(!javaHome.isNullOrEmpty())
    }

}