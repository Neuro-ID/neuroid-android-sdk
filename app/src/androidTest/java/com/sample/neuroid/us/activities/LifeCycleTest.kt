package com.sample.neuroid.us.activities

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.NIDSchema
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@ExperimentalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class LifeCycleTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<NIDCustomEventsActivity> =
        ActivityScenarioRule(NIDCustomEventsActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        NIDJobServiceManager.isSendEventsNowEnabled = false
        NeuroID.getInstance()?.isStopped()?.let {
            if (it) {
                NeuroID.getInstance()?.start()
            }
        }
        delay(500)
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /**
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun test13ValidateChangeScreenOrientation() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.setOrientationRight()
        device.setOrientationNatural()
        val eventType = "\"type\":\"WINDOW_ORIENTATION_CHANGE\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType, -1)
    }
}