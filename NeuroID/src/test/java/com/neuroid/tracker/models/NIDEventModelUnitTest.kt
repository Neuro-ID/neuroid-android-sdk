package com.neuroid.tracker.models

import org.junit.Assert.assertEquals
import org.junit.Test

class NIDEventModelUnitTest {
    @Test
    fun testToJSONSimple() {
        val event = NIDEventModel(type = "TEST_TYPE", ts = 1)
        val result = event.toJSONString()

        assertEquals(
            "{\"type\":\"TEST_TYPE\",\"accel\":{\"x\":null,\"y\":null,\"z\":null},\"ts\":1,\"gyro\":{\"x\":null,\"y\":null,\"z\":null}}",
            result,
        )
    }

    @Test
    fun testToJSONAdvanced() {
        val event =
            NIDEventModel(
                type = "TEST_TYPE",
                ts = 1,
                attrs =
                    listOf(
                        mapOf(
                            "key" to "value",
                        ),
                    ),
                tg = mapOf("key1" to "value1", "key2" to "value2"),
                dnt = false,
            )
        val result = event.toJSONString()

        assertEquals(
            "{\"tg\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"dnt\":false,\"type\":\"TEST_TYPE\",\"accel\":{\"x\":null,\"y\":null,\"z\":null},\"attrs\":[{\"key\":\"value\"}],\"ts\":1,\"gyro\":{\"x\":null,\"y\":null,\"z\":null}}",
            result,
        )
    }
}
