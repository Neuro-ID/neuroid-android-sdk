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
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Neuro ID: 26 UI Test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class NeuroIdUITest {

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() {
        Dispatchers.setMain(testDispatcher)
        NeuroID.getInstance()?.stop()
    }

    @ExperimentalCoroutinesApi
    @After
    fun resetDispatchers() {
        testScope.launch {
            getDataStoreInstance().clearEvents()
        }
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Validate CREATE_SESSION on start method
     */
    @Test
    fun test01ValidateCreateSession() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.stop()
        NeuroID.getInstance()?.start()
        Thread.sleep(500)

        val eventType = "\"type\":\"CREATE_SESSION\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateCreateSession - Events: $event")
        NIDLog.d("----> UITest", "----> validateCreateSession - Events: $event")
        assertThat(event).matches(NID_STRUCT_CREATE_SESSION)
    }

    /**
     * Validate REGISTER_TARGET on MainActivity class
     */
    @Test
    fun test02ValidateRegisterTargets() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(1000) //Wait a half second for create the MainActivity View

        val strEvents = getDataStoreInstance().getAllEvents()
        val eventsRegister = strEvents.filter { it.contains("\"type\":\"REGISTER_TARGET\"") }
        val event = eventsRegister.firstOrNull().orEmpty()

        NIDLog.d("----> UITest", "----> validateRegisterTargets - Events: $eventsRegister")

        assertThat(event).matches(NID_STRUCT_REGISTER_TARGET)
    }

    /**
     * Validate SET_USER_ID
     */
    @Test
    fun test03ValidateSetUserId() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.setUserID("UUID1234")
        Thread.sleep(500)

        val eventType = "\"type\":\"SET_USER_ID\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)

        NIDLog.d("----> UITest", "----> validateSetUserId - Event: [$event]")

        assertThat(event).matches(NID_STRUCT_USER_ID)
    }

    /**
     * Validate FORM_SUBMIT on NIDCustomEventsActivity class
     */
    @Test
    fun test04ValidateFormSubmit() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_submit))
            .perform(click())
        Thread.sleep(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)

        NIDLog.d("----> UITest", "----> validateFormSubmit - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_SUBMIT)
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun test05ValidateFormSubmitSuccess() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_success))
            .perform(click())
        Thread.sleep(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_SUCCESS\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateFormSubmitSuccess - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_SUCCESS)
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun test06ValidateFormSubmitFailure() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_failure))
            .perform(click())
        Thread.sleep(600)

        val eventType = "\"type\":\"APPLICATION_SUBMIT_FAILURE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)

        NIDLog.d("----> UITest", "----> validateFormSubmitFailure - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_ERROR)
    }

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     */
    @Test
    fun test07ValidateFormCustomEvent() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_custom_event))
            .perform(click())
        Thread.sleep(600)

        val eventType = "\"type\":\"CUSTOM_EVENT\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)

        NIDLog.d("----> UITest", "----> validateFormCustom - Event: $event")

        assertThat(event).matches(NID_STRUCT_CUSTOM_EVENT)
    }

    /**
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun test08ValidateLifecycleStart() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) //Wait a half second for create the MainActivity View
        val eventType = "\"type\":\"WINDOW_LOAD\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateLifecycleStart - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_LOAD)
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun test09ValidateLifecycleResume() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) //Wait a half second for create the MainActivity View

        val eventType = "\"type\":\"WINDOW_FOCUS\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)

        NIDLog.d("----> UITest", "----> validateLifecycleResume - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_FOCUS)
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun test10ValidateLifecyclePause() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        val eventType = "\"type\":\"WINDOW_BLUR\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType, 0)

        NIDLog.d("----> UITest", "----> validateLifecyclePause - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_BLUR)
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun test11ValidateLifecycleStop() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)
        Espresso.pressBack()
        Thread.sleep(500)

        val eventType = "\"type\":\"WINDOW_UNLOAD\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType, 0)
        NIDLog.d("----> UITest", "----> validateLifecycleStop - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_UNLOAD)
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun test12ValidateTouchStart() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val eventType = "\"type\":\"TOUCH_START\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateClickOnScreen - Event: $event")
        assertThat(event).matches(NID_STRUCT_TOUCH_START)
    }

    /**
     * Validate TOUCH_END when the user up finger on screen
     */
    @Test
    fun test13ValidateTouchEnd() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val eventType = "\"type\":\"TOUCH_END\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateEndClickScreen - Event: $event")
        assertThat(event).matches(NID_STRUCT_TOUCH_END)
    }

    /**
     * Validate TOUCH_MOVE when the user scroll on screen
     */
    @Test
    fun test14ValidateSwipeScreen() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.layout_main))
            .perform(swipeDown())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"TOUCH_MOVE\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateSwipeScreen - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_TOUCH_MOVE)
    }

    /**
     * Validate WINDOW_RESIZE when the user click on editText
     */
    @Test
    fun test15ValidateWindowsResize() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(1000)

        val eventType = "\"type\":\"WINDOW_RESIZE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType, 0)
        NIDLog.d("----> UITest", "----> validateWindowsResize - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_WINDOW_RESIZE)
    }

    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun test16ValidateFocusOnEditText() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val eventType = "\"type\":\"FOCUS\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateFocusOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_FOCUS)
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun test17ValidateBlurOnEditText() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(600)

        onView(withId(R.id.editText_password_field))
            .perform(click())
        Thread.sleep(600)

        val eventType = "\"type\":\"BLUR\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateBlurOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_BLUR)
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun test18ValidateInputText() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        val text = "Some text"
        onView(withId(R.id.editText_normal_field))
            .perform(typeText(text))
        Thread.sleep(500)

        val eventType = "\"type\":\"INPUT\""
        val event =
            validateEventCount(getDataStoreInstance().getAllEvents(), eventType, text.length)
        NIDLog.d("----> UITest", "----> validateInputText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_INPUT)
    }

    /**
     * Validate TEXT_CHANGE when the user type on editText and change focus
     */
    @Test
    fun test19ValidateTypeTextOnEditText() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        Thread.sleep(500)

        onView(withId(R.id.editText_password_field))
            .perform(click())
        Thread.sleep(500)

        val eventType = "\"type\":\"TEXT_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateTypeTextOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_TEXT_CHANGE)
    }

    /**
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun test20ValidateChangeScreenOrientation() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val device = UiDevice.getInstance(getInstrumentation())

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        device.setOrientationRight()
        Thread.sleep(500)
        device.setOrientationNatural()
        Thread.sleep(500)
        val eventType = "\"type\":\"WINDOW_ORIENTATION_CHANGE\""
        val events = getDataStoreInstance().getAllEvents()
        val event = validateEventCount(events, eventType, 0)
        NIDLog.d("----> UITest", "----> validateChangeScreenOrientation - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_WINDOW_ORIENTATION_CHANGE)
    }

    /**
     * Validate USER_INACTIVE when the user does not interact with the application for 30 seconds
     */
    @Test
    fun test21ValidateUserIsInactive() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(35000) // +1 second to wait write data

        val events = getDataStoreInstance().getAllEvents()
        val eventType = "\"type\":\"USER_INACTIVE\""
        val event = validateEventCount(events, eventType)
        NIDLog.d("----> UITest", "----> validateUserIsInactive - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_USER_INACTIVE)
    }

    /**
     * Validate the sending of data to the server correctly, if the return code of the server is
     * NID_OK_SERVICE the sending was successful
     */
    @ExperimentalCoroutinesApi
    @Test
    fun test22ValidateSendDataToService() = runBlockingTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        //Add some events:
        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        CoroutineScope(Dispatchers.IO).launch {
            val typeResponse = NIDServiceTracker.sendEventToServer(
                "key_live_vtotrandom_form_mobilesandbox",
                "https://api.neuro-id.com/v3/c",
                application
            )
            assertThat(typeResponse.first == NIDServiceTracker.NID_OK_SERVICE).isTrue()
        }
    }

}