package com.sample.neuroid.us

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun validateSendEvents() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val simulateEvent = NIDEventModel(type = WINDOW_LOAD, ts = System.currentTimeMillis())
        getDataStoreInstance(appContext).saveEvent(simulateEvent.getOwnJson())

        Truth.assertThat(
            NIDServiceTracker.sendEventToServer(
                "key_live_vtotrandom_form_mobilesandbox",
                appContext
            )
        ).isEqualTo(true)
    }

    @Test
    fun validateSaveEvents() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val simulateEventOne = NIDEventModel(type = "DEMO_EVENT_ONE", ts = System.currentTimeMillis())
        val simulateEventTwo = NIDEventModel(type = "DEMO_EVENT_TWO", ts = System.currentTimeMillis())
        val simulateEventThree = NIDEventModel(type = "DEMO_EVENT_THREE", ts = System.currentTimeMillis())

        val listEvents = listOf(simulateEventOne,simulateEventTwo, simulateEventThree)
        listEvents.forEach {
            getDataStoreInstance(appContext).saveEvent(it.getOwnJson())
        }

        val listSavedEvents = getDataStoreInstance(appContext).getAllEvents()

        Truth.assertThat(listSavedEvents.size == listEvents.size)
    }
}