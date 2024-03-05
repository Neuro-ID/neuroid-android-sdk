package com.sample.neuroid.us.activities

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.ResponseData
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
                val gson = Gson()

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
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun test13ValidateChangeScreenOrientation() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        device.setOrientationRight()
        device.setOrientationNatural()
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_ORIENTATION_CHANGE")
    }
}