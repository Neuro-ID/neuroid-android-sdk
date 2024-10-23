package com.neuroid.example

import junit.framework.TestCase.assertTrue

/**
 * record events received from the app through the mock webserver and
 * verify them for event integrity.
 */
class EventRecorder {
    private val eventBuffer = mutableListOf<EventModel>()

    fun addEvent(eventModel: EventModel) {
        eventBuffer.add(eventModel)
    }

    /**
     * for now we will simply count the number of events and compare
     * with an expected event count. In the future we should pass in a
     * JSON expected result and compare with the events received from
     * the app.
     */
    fun verifyEventList(expectedEventCount: Int) {
        var eventCount = 0
        println("printEventList --------start----------")
        eventBuffer.forEach { eventModel ->
            eventModel.jsonEvents.forEach { jsonEventModel ->
                eventCount ++
            }
        }
        println("printEventList--------end count: expected: $expectedEventCount, actual: $eventCount ----------")
        assertTrue("expectedEventCount: ${expectedEventCount} == eventCount: ${eventCount} ", eventCount == expectedEventCount)
        eventBuffer.clear()
    }
}