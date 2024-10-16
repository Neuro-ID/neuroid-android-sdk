package com.sample.neuroid.us.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
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
import org.hamcrest.core.Is.`is`
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.time.Duration

@ExperimentalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class DynamicActivityTest: MockServerTest() {

    @get:Rule
    var activityRule: ActivityScenarioRule<DynamicActivity> =
        ActivityScenarioRule(DynamicActivity::class.java)

    fun forceSendEvents(){
        // stop to force send all events in queue
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /*
    Actual Tests
     */

    @Test
    fun test01ValidateFormSubmit() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.btnAdd))
            .perform(click())
        Espresso.onView(ViewMatchers.withTagValue(`is`("etNewEditText"))).perform(click())
        Espresso.onView(ViewMatchers.withTagValue(`is`("etNewEditText"))).perform(pressKey(33))
        delay(2000)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.btnAddWithRegisterTarget))
            .perform(click())
        delay(2000)

        forceSendEvents()
        assertRequestBodyContains("REGISTER_TARGET")
    }
}