package com.neuroid.example

import junit.framework.TestCase.assertTrue

/**
 * record events received from the app through the mock webserver and
 * verify them for event integrity.
 */
class EventRecorder {
    private val eventBuffer = mutableListOf<EventModel>()

    data class RecorderComparison(
        val payloadCountExpected: Int,
        val payloadCountActual: Int,
        val expectedTypeCounts: Map<String, Int>,
        val actualTypeCounts: Map<String, Int>,
        val missingEventTypes: Map<String, Int>,
        val extraEventTypes: Map<String, Int>,
        val missingBySessionAndType: Map<String, Int>,
        val extraBySessionAndType: Map<String, Int>,
    ) {
        fun isMatch(): Boolean {
            return payloadCountExpected == payloadCountActual &&
                missingEventTypes.isEmpty() &&
                extraEventTypes.isEmpty() &&
                missingBySessionAndType.isEmpty() &&
                extraBySessionAndType.isEmpty()
        }
    }

    fun addEvent(eventModel: EventModel) {
        eventBuffer.add(eventModel)
    }

    fun clear() {
        eventBuffer.clear()
    }

    /**
     * for now we will simply count the number of events and compare
     * with an expected event count. In the future we should pass in a
     * JSON expected result and compare with the events received from
     * the app.
     */
    fun verifyEventList(expectedEventCount: Int, eventCountVariance: Int) {
        var eventCount = 0
        eventBuffer.forEach { eventModel ->
            eventModel.jsonEvents.forEach { jsonEventModel ->
                eventCount ++
            }
        }
        // some emulators will returns different events counts, we can approximate this check to
        // account for the variance here and get a ballpark result here
        assertTrue("expectedEventCount: ${expectedEventCount} ~ eventCount: ${eventCount} +-$eventCountVariance ",
            (eventCount >= expectedEventCount - eventCountVariance && eventCount <= expectedEventCount + eventCountVariance)
        )
        eventBuffer.clear()
    }
    fun verifyNoOrphanedAdvSignals(): List<String> {
        // group all payloads by session (userId), then check across all payloads per session
        val sessionEvents = mutableMapOf<String, MutableSet<String>>()
        eventBuffer.forEach { eventModel ->
            val key = eventModel.userId.orEmpty().ifBlank { eventModel.clientId }
            val types = sessionEvents.getOrPut(key) { mutableSetOf() }
            eventModel.jsonEvents.forEach { types.add(it.type) }
        }
        return sessionEvents
            .filter { (_, types) -> types.contains("ADVANCED_DEVICE_REQUEST") && types.size == 1 }
            .keys.toList()
    }

    fun hasReceivedEventType(type: String): Boolean {
        return eventBuffer.any { model -> model.jsonEvents.any { it.type == type } }
    }

    fun allReceivedEventTypes(): Set<String> {
        return eventBuffer.flatMap { model -> model.jsonEvents.map { it.type } }.toSet()
    }

    fun eventTypeCounts(): Map<String, Int> {
        return eventBuffer
            .flatMap { model -> model.jsonEvents.map { it.type } }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }

    fun payloadCount(): Int {
        return eventBuffer.size
    }

    fun compareTo(actual: EventRecorder): RecorderComparison {
        val expectedTypeCounts = eventTypeCounts()
        val actualTypeCounts = actual.eventTypeCounts()

        val expectedSessionTypeCounts = sessionAndTypeCounts()
        val actualSessionTypeCounts = actual.sessionAndTypeCounts()

        return RecorderComparison(
            payloadCountExpected = payloadCount(),
            payloadCountActual = actual.payloadCount(),
            expectedTypeCounts = expectedTypeCounts,
            actualTypeCounts = actualTypeCounts,
            missingEventTypes = mapDiff(expectedTypeCounts, actualTypeCounts),
            extraEventTypes = mapDiff(actualTypeCounts, expectedTypeCounts),
            missingBySessionAndType = mapDiff(expectedSessionTypeCounts, actualSessionTypeCounts),
            extraBySessionAndType = mapDiff(actualSessionTypeCounts, expectedSessionTypeCounts),
        )
    }

    private fun sessionAndTypeCounts(): Map<String, Int> {
        return eventBuffer
            .flatMap { model ->
                val sessionKey = model.userId.orEmpty().ifBlank { model.clientId }
                model.jsonEvents.map { event -> "$sessionKey::${event.type}" }
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }

    private fun mapDiff(expected: Map<String, Int>, actual: Map<String, Int>): Map<String, Int> {
        val keys = expected.keys + actual.keys
        return keys
            .mapNotNull { key ->
                val delta = (expected[key] ?: 0) - (actual[key] ?: 0)
                if (delta > 0) key to delta else null
            }
            .toMap()
            .toSortedMap()
    }
}