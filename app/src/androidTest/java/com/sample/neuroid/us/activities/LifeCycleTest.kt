package com.sample.neuroid.us.activities

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.NIDSchema
import com.sample.neuroid.us.ResponseData
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun test13ValidateChangeScreenOrientation() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        getDataStoreInstance().clearEvents()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        getDataStoreInstance().clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        device.setOrientationRight()
        delay(500)
        device.setOrientationNatural()
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_ORIENTATION_CHANGE")

    }
}