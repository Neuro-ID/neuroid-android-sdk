package com.sample.neuroid.us.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.NIDSchema
import com.sample.neuroid.us.R
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Neuro ID: 26 UI Test
 */
@ExperimentalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class NonAutomaticEventsTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<NIDCustomEventsActivity> =
        ActivityScenarioRule(NIDCustomEventsActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        NIDJobServiceManager.isSendEventsNowEnabled = false
        NeuroID.getInstance()?.stop()
    }


    /**
     * Validate FORM_SUBMIT on NIDCustomEventsActivity class
     */
    @Test
    fun test01ValidateFormSubmit() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500)
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_submit))
            .perform(ViewActions.click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType)
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun test02ValidateFormSubmitSuccess() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_success))
            .perform(ViewActions.click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_SUCCESS\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType)
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun test03ValidateFormSubmitFailure() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_failure))
            .perform(ViewActions.click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_FAILURE\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType)
    }

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     */
    @Test
    fun test04ValidateFormCustomEvent() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.button_send_custom_event))
            .perform(ViewActions.click())
        delay(1000)

        val eventType = "\"type\":\"CUSTOM_EVENT\""
        val events = getDataStoreInstance().getAllEvents()
        NIDSchema().validateSchema(events)
        NIDSchema().validateEvents(events, eventType)
    }

}