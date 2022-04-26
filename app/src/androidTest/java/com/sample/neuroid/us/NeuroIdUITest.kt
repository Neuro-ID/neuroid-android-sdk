package com.sample.neuroid.us

import android.app.Application
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Neuro ID: 11 UI Test
 */
@RunWith(AndroidJUnit4::class)
class NeuroIdUITest {

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() {
        Dispatchers.setMain(testDispatcher)
        NeuroID.getInstance().stop()
    }

    @ExperimentalCoroutinesApi
    @After
    fun resetDispatchers() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Validate CREATE_SESSION on start method
     */
    @Test
    fun validateCreateSession() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance().start()
        Thread.sleep(500)
        NeuroID.getInstance().stop()

        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"CREATE_SESSION\"") }

        NIDLog.d("----> UITest", "----> validateCreateSession - Events: $event")

        assertThat(event).matches(NID_STRUCT_CREATE_SESSION)
    }

    /**
     * Validate SET_USER_ID
     */
    @Test
    fun validateSetUserId() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance().start()
        NeuroID.getInstance().setUserID("UUID1234")
        Thread.sleep(500)
        NeuroID.getInstance().stop()

        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"SET_USER_ID\"") }

        NIDLog.d("----> UITest", "----> validateSetUserId - Event: [$event]")

        assertThat(event).matches(NID_STRUCT_USER_ID)
    }

    /**
     * Validate REGISTER_TARGET on MainActivity class
     */
    @Test
    fun validateRegisterTargets() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        val strEvents = getDataStoreInstance().getAllEvents()
        val eventsRegister = strEvents.filter { it.contains("\"type\":\"REGISTER_TARGET\"") }
        val event = eventsRegister.firstOrNull().orEmpty()

        NIDLog.d("----> UITest", "----> validateRegisterTargets - Events: $eventsRegister")

        assertThat(event).matches(NID_STRUCT_REGISTER_TARGET)
    }

    /**
     * Validate SLIDER_CHANGE on NIDOnlyOneFragment class
     */
    @Test
    fun validateSliderChange() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.layout_scroll)).perform(
            swipeUp()
        )

        onView(withId(R.id.layout_scroll)).perform(
            swipeUp()
        )

        Thread.sleep(500)

        onView(withId(R.id.seekBar_one)).perform(
            setValue(50)
        )

        Thread.sleep(500)

        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"SLIDER_CHANGE\"") }

        NIDLog.d("----> UITest", "----> validateSliderChange - Event: [$event]")


        assertThat(event).matches(NID_STRUCT_SLIDER_CHANGE)
    }

    /**
     * Validate FORM_SUBMIT on NIDCustomEventsActivity class
     */
    @Test
    fun validateFormSubmit() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_submit))
            .perform(click())
        Thread.sleep(600)


        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"FORM_SUBMIT\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateFormSubmit - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_SUBMIT)
    }

    /**
     * Validate FORM_SUBMIT_SUCCESS on NIDCustomEventsActivity class
     */
    @Test
    fun validateFormSubmitSuccess() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_success))
            .perform(click())
        Thread.sleep(600)


        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"FORM_SUBMIT_SUCCESS\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateFormSubmitSuccess - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_SUCCESS)
    }

    /**
     * Validate FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun validateFormSubmitFailure() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_failure))
            .perform(click())
        Thread.sleep(600)


        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"FORM_SUBMIT_FAILURE\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateFormSubmitFailure - Event: $event")

        assertThat(event).matches(NID_STRUCT_FORM_ERROR)
    }

    /**
     * Validate CUSTOM_EVENT on NIDCustomEventsActivity class
     */
    @Test
    fun validateFormCustomEvent() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_custom_event))
            .perform(click())
        Thread.sleep(600)


        val strEvents = getDataStoreInstance().getAllEvents()
        val event = strEvents.firstOrNull { it.contains("\"type\":\"CUSTOM_EVENT\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateFormCustom - Event: $event")

        assertThat(event).matches(NID_STRUCT_CUSTOM_EVENT)
    }

    /**
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun validateLifecycleStart() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) //Wait a half second for create the MainActivity View
        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_LOAD\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateLifecycleStart - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_LOAD)
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun validateLifecycleResume() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) //Wait a half second for create the MainActivity View
        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_FOCUS\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateLifecycleResume - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_FOCUS)
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun validateLifecyclePause() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_BLUR\"") }.orEmpty()

        NIDLog.d("----> UITest", "----> validateLifecyclePause - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_BLUR)
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun validateLifecycleStop() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)
        Espresso.pressBack()
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()

        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_UNLOAD\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateLifecycleStop - Event: $event")

        assertThat(event).matches(NID_STRUCT_WINDOW_UNLOAD)
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun validateTouchStart() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.textView_label_one))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"TOUCH_START\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateClickOnScreen - Event: $event")
        assertThat(event).matches(NID_STRUCT_TOUCH_START)
    }

    /**
     * Validate TOUCH_END when the user up finger on screen
     */
    @Test
    fun validateTouchEnd() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.textView_label_one))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()

        val event = events.firstOrNull { it.contains("\"type\":\"TOUCH_END\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateEndClickScreen - Event: $event")
        assertThat(event).matches(NID_STRUCT_TOUCH_END)
    }

    /**
     * Validate TOUCH_MOVE when the user scroll on screen
     */
    @Test
    fun validateSwipeScreen() {
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
    fun validateWindowsResize() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(1000)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_RESIZE\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateWindowsResize - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_WINDOW_RESIZE)
    }

    /**
     * Validate WINDOW_ORIENTATION_CHANGE when the user move device portrait or landscape
     */
    @Test
    fun validateChangeScreenOrientation() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val device = UiDevice.getInstance(getInstrumentation())

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        device.setOrientationRight()
        Thread.sleep(500)
        device.setOrientationNatural()
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"WINDOW_ORIENTATION_CHANGE\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateChangeScreenOrientation - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_WINDOW_ORIENTATION_CHANGE)
    }

    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun validateFocusOnEditText() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"FOCUS\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateFocusOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_FOCUS)
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun validateBlurOnEditText() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(600)

        onView(withId(R.id.editText_password_field))
            .perform(click())
        Thread.sleep(600)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"BLUR\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateBlurOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_BLUR)
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun validateInputText() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"INPUT\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateInputText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_INPUT)
    }

    /**
     * Validate TEXT_CHANGE when the user type on editText and change focus
     */
    @Test
    fun validateTypeTextOnEditText() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        Thread.sleep(500)

        onView(withId(R.id.editText_password_field))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"TEXT_CHANGE\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateTypeTextOnEditText - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_TEXT_CHANGE)
    }

    /**
     * Validate RADIO_CHANGE when the user click on it
     */
    @Test
    fun validateRadioChange() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.radioButton_one))
            .perform(click())

        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"RADIO_CHANGE\"") }.orEmpty()
        NIDLog.d("----> UITest", "----> validateRadioChange - Event: $event")
        assertThat(event).matches(NID_STRUCT_RADIO_CHANGE)
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun validateCheckBox() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.check_one))
            .perform(click())

        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"CHECKBOX_CHANGE\"") }
        NIDLog.d("----> UITest", "----> validateClickControlViews - Event: $event")
        assertThat(event).matches(NID_STRUCT_CHECKBOX_CHANGE)
    }

    /**
     * Validate SELECT_CHANGE when the user select one item on list
     */
    @Test
    fun validateComboBoxSelectItem() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.button_show_activity_fragments))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.spinner_example))
            .perform(click())
        onData(anything()).atPosition(1).perform(click())

        Thread.sleep(1000)

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"SELECT_CHANGE\"") }
        NIDLog.d("----> UITest", "----> validateComboBoxSelectItem - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_SELECT_CHANGE)
    }

    /**
     * Validate USER_INACTIVE when the user does not interact with the application for 30 seconds
     */
    @Test
    fun validateUserIsInactive() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(31000) // +1 second to wait write data

        val events = getDataStoreInstance().getAllEvents()
        val event = events.firstOrNull { it.contains("\"type\":\"USER_INACTIVE\"") }
        NIDLog.d("----> UITest", "----> validateUserIsInactive - Event: [$event]")
        assertThat(event).matches(NID_STRUCT_USER_INACTIVE)
    }

    /**
     * Validate the sending of data to the server correctly, if the return code of the server is
     * NID_OK_SERVICE the sending was successful
     */
    @ExperimentalCoroutinesApi
    @Test
    fun validateSendDataToService() = runBlockingTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        //Add some events:
        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        CoroutineScope(Dispatchers.IO).launch {
            val typeResponse = NIDServiceTracker.sendEventToServer(
                "key_live_vtotrandom_form_mobilesandbox",
                application
            )
            assertThat(typeResponse.first == NIDServiceTracker.NID_OK_SERVICE).isTrue()
        }
    }

    fun setValue(value: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> {
                return isAssignableFrom(SeekBar::class.java)
            }

            override fun getDescription(): String {
                return "Set SeekBar value to $value"
            }

            override fun perform(uiController: UiController?, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = value
            }
        }
    }
}