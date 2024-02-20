package com.neuroid.tracker

import com.neuroid.tracker.models.NIDEventModel
import org.junit.Assert.assertEquals
import org.junit.Test

class NIDEventModelUnitTest {
    @Test
    fun testToJSONSimple() {
        val event = NIDEventModel(type = "TEST_TYPE", ts = 1)
        val result = event.getOwnJson()

        assertEquals("{\"type\":\"TEST_TYPE\",\"ts\":1}", result)
    }

    @Test
    fun testToJSONAdvanced() {
        val event = NIDEventModel(
            type = "TEST_TYPE",
            ts = 1,
            attrs = listOf(
                mapOf(
                    "key" to "value"
                )
            ),
            tg = mapOf("key1" to "value1", "key2" to "value2"),
            dnt = false
            )
        val result = event.getOwnJson()

        assertEquals("{\"tg\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"dnt\":false,\"type\":\"TEST_TYPE\",\"attrs\":[{\"key\":\"value\"}],\"ts\":1}", result)
    }
}