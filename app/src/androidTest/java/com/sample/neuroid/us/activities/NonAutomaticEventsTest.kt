package com.sample.neuroid.us.activities

import android.location.LocationListener
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.CoroutineScopeAdapter
import com.neuroid.tracker.utils.LocationListenerCreator
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.ResponseData
import com.sample.neuroid.us.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
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
        server.enqueue(MockResponse().setBody("").setResponseCode(200))
        NeuroID.getInstance()?.setTestURL(url)

        NeuroID.getInstance()?.isStopped()?.let {
            if (it) {
                NeuroID.getInstance()?.start()
            }
        }
        delay(500)
    }

    @After
    fun resetDispatchers() = runTest {
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
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
            var foundEventFlag = false
            for (i in 0 until request) {
                var  req  = server.takeRequest()
                val body = req.body.readUtf8().toString()
                val gson = GsonBuilder()
                    .registerTypeAdapter(LocationListener::class.java, LocationListenerCreator())
                    .registerTypeAdapter(CoroutineScope::class.java, CoroutineScopeAdapter())
                    .create()

                val jsonObject: ResponseData? = gson.fromJson(body, ResponseData::class.java)

                val foundEvent = jsonObject?.jsonEvents?.find { event -> event.type == eventType }
                if (foundEvent != null) {
                    foundEventFlag = true
                }
            }

            assert(foundEventFlag == true) {
                "$eventType not found in request object (total of $request objects searched)"
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
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_submit))
            .perform(ViewActions.click())

        forceSendEvents()
        assertRequestBodyContains("APPLICATION_SUBMIT")
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun test02ValidateFormSubmitSuccess() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_success))
            .perform(ViewActions.click())


        forceSendEvents()
        assertRequestBodyContains("APPLICATION_SUBMIT_SUCCESS")
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun test03ValidateFormSubmitFailure() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) //Wait a half second for create the MainActivity View
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_send_form_failure))
            .perform(ViewActions.click())

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