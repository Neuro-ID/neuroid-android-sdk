package com.sample.neuroid.us

import android.app.Application
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
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
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"CREATE_SESSION\"") }

        NIDLog.d("----> UITest", "----> validateCreateSession - Event: [$event]")

        assertThat(events.contains("\"CREATE_SESSION\"")).isTrue()
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
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"SET_USER_ID\"") }

        NIDLog.d("----> UITest", "----> validateSetUserId - Event: [$event]")

        assertThat(events.contains("\"SET_USER_ID\"")).isTrue()
    }

    /**
     * Validate REGISTER_TARGET on MainActivity class
     */
    @Test
    fun validateRegisterTargets() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsRegister = strEvents.filter { it.contains("\"REGISTER_TARGET\"") }
        NIDLog.d("----> UITest", "----> validateRegisterTargets - Events: [$eventsRegister]")

        val numberOfInputs = events.splitToSequence("\"REGISTER_TARGET\"").count() - 1
        NIDLog.d("----> UITest", "----> numberOfInputs: $numberOfInputs")

        assertThat(numberOfInputs == 5).isTrue()
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

        onView(withId(R.id.layout_nested))
            .perform(swipeUp())

        onView(withId(R.id.layout_nested))
            .perform(swipeUp())

        Thread.sleep(500)

        onView(withId(R.id.seekBar_one)).perform(
            setValue(50)
        )

        Thread.sleep(500)

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"SLIDER_CHANGE\"") }

        NIDLog.d("----> UITest", "----> validateSliderChange - Event: [$event]")


        assertThat(events.contains("\"SLIDER_CHANGE\"")).isTrue()
    }

    /**
     * Validate FORM_SUBMIT, FORM_SUBMIT_SUCCESS and FORM_SUBMIT_FAILURE on NIDCustomEventsActivity class
     */
    @Test
    fun validateNonAutomaticEvents() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_no_automatic_events))
            .perform(click())
        Thread.sleep(400)

        onView(withId(R.id.button_send_form_submit))
            .perform(click())
        Thread.sleep(600)

        onView(withId(R.id.button_send_form_success))
            .perform(click())
        Thread.sleep(600)

        onView(withId(R.id.button_send_form_failure))
            .perform(click())
        Thread.sleep(600)

        onView(withId(R.id.button_send_custom_event))
            .perform(click())
        Thread.sleep(600)

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsCustom = strEvents.filter {
            it.contains("\"FORM_SUBMIT\"") ||
                    it.contains("\"FORM_SUBMIT_SUCCESS\"")  ||
                    it.contains("\"FORM_SUBMIT_FAILURE\"")  ||
                    it.contains("\"CUSTOM_EVENT\"")
        }
        NIDLog.d("----> UITest", "----> validateNonAutomaticEvents - Events: [$eventsCustom]")

        assertThat(
                    events.contains("\"FORM_SUBMIT\"") &&
                    events.contains("\"FORM_SUBMIT_SUCCESS\"") &&
                    events.contains("\"FORM_SUBMIT_FAILURE\"") &&
                    events.contains("\"CUSTOM_EVENT\"")
        ).isTrue()
    }

    /**
     * Validate WINDOW_LOAD and WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun validateLifecycleStartAndResume() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val statusList = listOf(
            "\"WINDOW_LOAD\"",
            "\"WINDOW_FOCUS\""
        )
        Thread.sleep(500) //Wait a half second for create the MainActivity View
        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsLifeCycle = strEvents.filter {
            it.contains("\"WINDOW_LOAD\"") ||
                    it.contains("\"WINDOW_FOCUS\"")
        }
        NIDLog.d("----> UITest", "----> validateLifecycleStartAndResume - Events: [$eventsLifeCycle]")

        assertThat(statusList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate WINDOW_BLUR and WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun validateLifecyclePauseAndStop() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val statusList = listOf(
            "\"WINDOW_BLUR\"",
            "\"WINDOW_UNLOAD\""
        )
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsLifeCycle = strEvents.filter {
            it.contains("\"WINDOW_BLUR\"") ||
                    it.contains("\"WINDOW_UNLOAD\"")
        }
        NIDLog.d("----> UITest", "----> validateLifecyclePauseAndStop - Events: [$eventsLifeCycle]")

        assertThat(statusList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate TOUCH_START and TOUCH_END when the user click on screen
     */
    @Test
    fun validateClickOnScreen() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val eventsList = listOf(
            "\"TOUCH_START\"",
            "\"TOUCH_END\""
        )
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.textView_label_one))
            .perform(click())
        Thread.sleep(500)

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsTouch = strEvents.filter {
            it.contains("\"TOUCH_START\"") ||
                    it.contains("\"TOUCH_END\"")
        }
        NIDLog.d("----> UITest", "----> validateClickOnScreen - Events: [$eventsTouch]")
        assertThat(eventsList.all { events.contains(it) }).isTrue()
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"TOUCH_MOVE\"") }
        NIDLog.d("----> UITest", "----> validateSwipeScreen - Event: [$event]")
        assertThat(events.contains("\"TOUCH_MOVE\"")).isTrue()
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
        Thread.sleep(500)

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"WINDOW_RESIZE\"") }
        NIDLog.d("----> UITest", "----> validateWindowsResize - Event: [$event]")
        assertThat(events.contains("\"WINDOW_RESIZE\"")).isTrue()
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"WINDOW_ORIENTATION_CHANGE\"") }
        NIDLog.d("----> UITest", "----> validateChangeScreenOrientation - Event: [$event]")
        assertThat(events.contains("\"WINDOW_ORIENTATION_CHANGE\"")).isTrue()
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"FOCUS\"") }
        NIDLog.d("----> UITest", "----> validateFocusOnEditText - Event: [$event]")
        assertThat(events.contains("\"FOCUS\"")).isTrue()
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"BLUR\"") }
        NIDLog.d("----> UITest", "----> validateBlurOnEditText - Event: [$event]")
        assertThat(events.contains("\"BLUR\"")).isTrue()
    }

    /**
     * Validate TEXT_CHANGE when the user type on editText
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"TEXT_CHANGE\"") }
        NIDLog.d("----> UITest", "----> validateTypeTextOnEditText - Event: [$event]")
        assertThat(events.contains("\"TEXT_CHANGE\"")).isTrue()
    }

    /**
     * Validate RADIO_CHANGE, CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun validateClickControlViews() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val eventsList = listOf(
            "\"RADIO_CHANGE\"",
            "\"CHECKBOX_CHANGE\""
        )

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.radioButton_one))
            .perform(click())

        onView(withId(R.id.check_one))
            .perform(click())

        Thread.sleep(1000)

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val eventsControls = strEvents.filter {
            it.contains("\"RADIO_CHANGE\"") ||
                    it.contains("\"CHECKBOX_CHANGE\"")
        }
        NIDLog.d("----> UITest", "----> validateClickControlViews - Events: [$eventsControls]")
        assertThat(eventsList.all { events.contains(it) }).isTrue()
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

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"SELECT_CHANGE\"") }
        NIDLog.d("----> UITest", "----> validateComboBoxSelectItem - Event: [$event]")
        assertThat(events.contains("\"SELECT_CHANGE\"")).isTrue()
    }

    /**
     * Validate USER_INACTIVE when the user does not interact with the application for 30 seconds
     */
    @Test
    fun validateUserIsInactive() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(31000) // +1 second to wait write data

        val strEvents = getDataStoreInstance().getAllEvents()
        val events = strEvents.joinToString(",")
        val event = strEvents.firstOrNull { it.contains("\"USER_INACTIVE\"") }
        NIDLog.d("----> UITest", "----> validateUserIsInactive - Event: [$event]")
        assertThat(events.contains("USER_INACTIVE")).isTrue()
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
                return ViewMatchers.isAssignableFrom(SeekBar::class.java)
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