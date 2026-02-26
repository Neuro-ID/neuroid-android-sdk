package com.neuroid.tracker.models

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationMetaDataTest {
    @Test
    fun test_toList_containsNewParameters() {
        val metaData = ApplicationMetaData(
            versionName = "1.2.3",
            versionNumber = 123,
            packageName = "com.example.app",
            applicationName = "ExampleApp",
            hostRNVersion = "0.72.0",
            hostMinSDKLevel = 21,
        )
        val list = metaData.toList()
        val map = list.associate { it["n"] to it["v"] }
        assertEquals("0.72.0", map["hostRNVersion"])
        assertEquals(21, map["hostMinSDKLevel"])
        assertEquals("1.2.3", map["versionName"])
        assertEquals(123, map["versionNumber"])
        assertEquals("com.example.app", map["packageName"])
        assertEquals("ExampleApp", map["applicationName"])
    }

    @Test
    fun test_toList_defaults() {
        val metaData = ApplicationMetaData(
            versionName = "2.0.0",
            versionNumber = 200,
            packageName = "com.test.default",
            applicationName = "TestDefault"
        )
        val list = metaData.toList()
        val map = list.associate { it["n"] to it["v"] }
        assertEquals("", map["hostRNVersion"])
        assertEquals(-1, map["hostMinSDKLevel"])
    }
}

