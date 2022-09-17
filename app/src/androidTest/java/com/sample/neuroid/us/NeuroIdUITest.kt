package com.sample.neuroid.us

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Neuro ID: 26 UI Test
 */
@OptIn(ExperimentalCoroutinesApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class NeuroIdUITest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        NeuroID.getInstance()?.stop()
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
    }

    /**
     * Validate CREATE_SESSION on start method
     */
    @Test
    fun test01ValidateCreateSession() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.stop()
        NeuroID.getInstance()?.start()
        delay(500)

        val eventType = "\"type\":\"CREATE_SESSION\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
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

        val eventType = "\"type\":\"REGISTER_TARGET\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, -1)
    }

    /**
     * Validate SET_USER_ID
     */
    @Test
    fun test03ValidateSetUserId() = runTest {
        NeuroID.getInstance()?.setUserID("UUID1234")
        delay(500)
        val eventType = "\"type\":\"SET_USER_ID\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate FORM_SUBMIT on NIDCustomEventsActivity class
     */
    @Test
    fun test04ValidateFormSubmit() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500)

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        delay(500)

        onView(withId(R.id.button_send_form_submit))
            .perform(click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun test05ValidateFormSubmitSuccess() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        delay(400)

        onView(withId(R.id.button_send_form_success))
            .perform(click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_SUCCESS\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun test06ValidateFormSubmitFailure() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        delay(400)

        onView(withId(R.id.button_send_form_failure))
            .perform(click())
        delay(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_FAILURE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     */
    @Test
    fun test07ValidateFormCustomEvent() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        delay(400)

        onView(withId(R.id.button_send_custom_event))
            .perform(click())
        delay(600)

        val eventType = "\"type\":\"CUSTOM_EVENT\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun test08ValidateLifecycleStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) //Wait a half second for create the MainActivity View
        val eventType = "\"type\":\"WINDOW_LOAD\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun test09ValidateLifecycleResume() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) //Wait a half second for create the MainActivity View

        val eventType = "\"type\":\"WINDOW_FOCUS\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun test10ValidateLifecyclePause() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)
        Espresso.pressBack()
        delay(500)
        val eventType = "\"type\":\"WINDOW_BLUR\""
        //TODO Check This event behavior
        //NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun test11ValidateLifecycleStop() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)
        Espresso.pressBack()
        delay(500)

        val eventType = "\"type\":\"WINDOW_UNLOAD\""
        //TODO Check This event behavior
        //NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun test12ValidateTouchStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(500)

        val eventType = "\"type\":\"TOUCH_START\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate TOUCH_END when the user up finger on screen
     */
    @Test
    fun test13ValidateTouchEnd() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(500)

        val eventType = "\"type\":\"TOUCH_END\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate TOUCH_MOVE when the user scroll on screen
     */
    @Test
    fun test14ValidateSwipeScreen() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.layout_main))
            .perform(swipeDown())
        delay(500)

        val eventType = "\"type\":\"TOUCH_MOVE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, -1)
    }

    /**
     * Validate WINDOW_RESIZE when the user click on editText
     */
    @Test
    fun test15ValidateWindowsResize() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        val eventType = "\"type\":\"WINDOW_RESIZE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun test20ValidateChangeScreenOrientation() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val device = UiDevice.getInstance(getInstrumentation())

        delay(500) // When you go to the next test, the activity is destroyed and recreated
        device.setOrientationRight()
        delay(500)
        device.setOrientationNatural()
        delay(500)
        val eventType = "\"type\":\"WINDOW_ORIENTATION_CHANGE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, -1)
    }

    /**
     * Validate USER_INACTIVE when the user does not interact with the application for 30 seconds
     */
    @Test
    fun test21ValidateUserIsInactive() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(35_000) // +1 second to wait write data
        val eventType = "\"type\":\"USER_INACTIVE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate the sending of data to the server correctly, if the return code of the server is
     * NID_OK_SERVICE the sending was successful
     */
    @Test
    fun test22ValidateSendDataToService() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        //Add some events:
        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        CoroutineScope(Dispatchers.IO).launch {
            val typeResponse = NIDServiceTracker.sendEventToServer(
                "key_live_vtotrandom_form_mobilesandbox",
                NeuroID.ENDPOINT_PRODUCTION,
                application
            )
            assertThat(typeResponse.first == NIDServiceTracker.NID_OK_SERVICE).isTrue()
        }
    }

}