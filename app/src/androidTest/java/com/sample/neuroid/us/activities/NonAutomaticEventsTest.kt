package com.sample.neuroid.us.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.MockServerTest
import com.sample.neuroid.us.R
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.time.Duration

/**
 * Neuro ID: 26 UI Test
 */
@ExperimentalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class NonAutomaticEventsTest: MockServerTest() {

    @get:Rule
    var activityRule: ActivityScenarioRule<NIDCustomEventsActivity> =
        ActivityScenarioRule(NIDCustomEventsActivity::class.java)

    /*
   Helper Test Functions
    */

    fun forceSendEvents(){
        // stop to force send all events in queue
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /*
    Actual Tests
     */

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     * Ignore this one and move all of these to SDK as a unit test.
     */
    @Test
    @Ignore
    fun test04ValidateFormCustomEvent() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.button_send_custom_event))
            .perform(ViewActions.click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("CUSTOM_EVENT")
    }

}