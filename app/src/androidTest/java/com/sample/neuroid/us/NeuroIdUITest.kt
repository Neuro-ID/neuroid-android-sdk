package com.sample.neuroid.us

import android.os.Looper
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


data class ResponseData(
    val siteId: String,
    val userId: String,
    val clientId: String,
    val identityId: String,
    val registeredUserId: String,
    val pageTag: String,
    val pageId: String,
    val tabId: String,
    val responseId: String,
    val url: String,
    val jsVersion: String,
    val sdkVersion: String,
    val environment: String,
    val jsonEvents: List<NIDEventModel>
)

/**
 * Neuro ID: 26 UI Test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class NeuroIdUITest {
    val server = MockWebServer()
    var uiDevice: UiDevice? = null

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Grant permission using UIAutomator
        val allowButton = uiDevice?.findObject(UiSelector().text("ALLOW")) ?: uiDevice?.findObject(
            UiSelector().text("Allow")
        )
        if (allowButton != null) {
            if (allowButton.exists()) {
                allowButton.click()
            }
        }
        server.start()
        val url = server.url("/c/").toString()
        NeuroID.getInstance()?.setTestURL(url)
        server.enqueue(MockResponse().setBody("").setResponseCode(200))

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
        var request = server.requestCount
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
     * Validate CREATE_SESSION on start method
     */
    @Test
    fun test01ValidateCreateSession() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("CREATE_SESSION")

//        NIDSchema().validateEvents(events, eventType)
    }

    /**
     * Validate REGISTER_TARGET on MainActivity class
     */
    @Test
    fun test02ValidateRegisterTargets() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(1000) //Wait a half second for create the MainActivity View

        forceSendEvents()
        assertRequestBodyContains("REGISTER_TARGET")
    }

    /**
     * Validate SET_USER_ID after sdk is started
     */
    @Test
    fun test03ValidateSetUserId() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.setUserID("UUID1234")
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_USER_ID")
    }

    /**
     * Validate SET_REGISTERED_USER_ID after sdk is started
     */
    @Test
    fun test03aValidateSetRegisteredUserId() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.setRegisteredUserID("UUID1234")
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_REGISTERED_USER_ID")
    }

    /**
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun test04ValidateLifecycleStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_LOAD")
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun test05ValidateLifecycleResume() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        forceSendEvents()
        assertRequestBodyContains("WINDOW_FOCUS")
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun test06ValidateLifecyclePause() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)
        Espresso.pressBack()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_BLUR")
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun test07ValidateLifecycleStop() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.pressBack()
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_UNLOAD")
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun test08ValidateTouchStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(200) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(200)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("TOUCH_START")
    }

    /**
     * Validate TOUCH_END when the user up finger on screen
     */
    @Test
    fun test09ValidateTouchEnd() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(500)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("TOUCH_END")
    }

    /**
     * Validate WINDOW_FOCUS when the user swipes on screen
     */
    @Test
    fun test11ValidateSwipeScreen() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        // TODO
        // Implement swipe test
    }

    /**
     * Validate WINDOW_RESIZE when the user click on editText
     */
    @Test
    fun test12ValidateWindowsResize() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(500)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("WINDOW_RESIZE")
    }



    /**
     * Validate on TOUCH_START that the input is registered
     */
    @Test
    fun test13ValidateTouchStartAddsRegisterEvent() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        delay(500)
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        forceSendEvents()
        assertRequestBodyContains("REGISTER_TARGET")
    }

    /**
     * Validate SET_USER_ID when sdk is not started
     */
    @Test
    fun test14ValidateSetUserIdPreStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        NeuroID.getInstance()?.stop()
        delay(500)
        NeuroID.getInstance()?.setUserID("UUID123")
        delay(500)
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_USER_ID")
    }

    /**
    * Validate SET_REGISTERED_USER_ID when sdk is not started
    */
    @Test
    fun test15ValidateSetRegisteredUserIdPreStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.stop()
        delay(500)
        NeuroID.getInstance()?.setRegisteredUserID("UUID1231212")
        delay(1500)
        NeuroID.getInstance()?.start()
        delay(500)

        forceSendEvents()
        assertRequestBodyContains("SET_REGISTERED_USER_ID")
    }
}






