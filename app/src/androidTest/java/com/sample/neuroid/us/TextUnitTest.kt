package com.sample.neuroid.us

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class TextUnitTest {
    val server = MockWebServer()
    var uiDevice: UiDevice? = null

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() = runTest {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Grant permission using UIAutomator
        var allowButton = uiDevice?.findObject(UiSelector().text("ALLOW")) ?: uiDevice?.findObject(
            UiSelector().text("Allow")
        )
        if (allowButton != null) {
            if (allowButton.exists()) {
                allowButton.click()
            }
        }
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
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun test01ValidateFocusOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        forceSendEvents()
        assertRequestBodyContains("FOCUS")
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun test02ValidateBlurOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.editText_password_field))
            .perform(ViewActions.click())

        forceSendEvents()
        assertRequestBodyContains("BLUR")
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun test03ValidateInputText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)

        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        val text = "Some text"
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.typeText(text))

        forceSendEvents()
        assertRequestBodyContains("INPUT")
    }
}