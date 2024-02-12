package com.sample.neuroid.us.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.NIDSchema
import com.sample.neuroid.us.R
import com.sample.neuroid.us.ResponseData
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
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
    val server = MockWebServer()

    @get:Rule
    var activityRule: ActivityScenarioRule<NIDCustomEventsActivity> =
        ActivityScenarioRule(NIDCustomEventsActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        server.start()
        val url = server.url("/c/").toString()
        NeuroID.getInstance()?.setTestURL(url)

        NeuroID.getInstance()?.stop()
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
        server.shutdown()
    }

    /*
   Helper Test Functions
    */

    fun forceSendEvents(){
        // stop to force send all events in queue
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    fun assertRequestBodyContains(eventType:String){
        val request = server.requestCount
        if (request >0){
            val req = server.takeRequest()
            val body = req.body.readUtf8().toString()
            val gson = Gson()

            val jsonObject: ResponseData? = gson.fromJson(body, ResponseData::class.java)

            jsonObject?.jsonEvents?.forEach {
                println("ev ${it.type}")
            }

            val foundEvent = jsonObject?.jsonEvents?.find { event -> event.type == eventType }
            assert(foundEvent != null) {
                "$eventType not found in request object"
            }
        } else {
            assert(false) {
                "Failed to send request from SDK"
            }
        }

    }

    /*
    Actual Tests
     */


    /**
     * Validate FORM_SUBMIT on NIDCustomEventsActivity class
     */
    @Test
    fun test01ValidateFormSubmit() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        delay(500)
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_submit))
            .perform(ViewActions.click())
        delay(600)

        forceSendEvents()
        assertRequestBodyContains("APPLICATION_SUBMIT")
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun test02ValidateFormSubmitSuccess() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_success))
            .perform(ViewActions.click())
        delay(600)


        forceSendEvents()
        assertRequestBodyContains("APPLICATION_SUBMIT_SUCCESS")
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun test03ValidateFormSubmitFailure() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_failure))
            .perform(ViewActions.click())
        delay(600)

        forceSendEvents()
        assertRequestBodyContains("APPLICATION_SUBMIT_FAILURE")
    }

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     * Ignore this one and move all of these to SDK as a unit test.
     */
    @Test
    @Ignore
    fun test04ValidateFormCustomEvent() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        getDataStoreInstance().clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.button_send_custom_event))
            .perform(ViewActions.click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("CUSTOM_EVENT")
    }

}