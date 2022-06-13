package com.sample.neuroid.us

import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class ComponentsTest {
    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

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
        NeuroID.getInstance().stop()
    }

    @ExperimentalCoroutinesApi
    @After
    fun resetDispatchers() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01ValidateCheckBox() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.check_one))
            .perform(click())

        Thread.sleep(500)

        val eventType = "\"type\":\"CHECKBOX_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateClickControlViews - Event: $event")
        Truth.assertThat(event).matches(NID_STRUCT_CHECKBOX_CHANGE)
    }

    /**
     * Validate RADIO_CHANGE when the user click on it
     */
    @Test
    fun test02ValidateRadioChange() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(ViewActions.click())
        Thread.sleep(500)

        onView(withId(R.id.radioButton_one))
            .perform(click())

        Thread.sleep(500)

        val eventType = "\"type\":\"RADIO_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateRadioChange - Event: $event")
        Truth.assertThat(event).matches(NID_STRUCT_RADIO_CHANGE)
    }

    /**
     * Validate SWITCH_CHANGE when the user click on it
     */
    @Test
    fun test03ValidateSwitch() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.switch_three)).perform(
            scrollTo()
        )
        Thread.sleep(500)
        onView(withId(R.id.switch_one))
            .perform(click())

        Thread.sleep(500)

        val eventType = "\"type\":\"SWITCH_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateRadioChange - Event: $event")
        Truth.assertThat(event).matches(NID_STRUCT_SWITCH_CHANGE)
    }

    /**
     * Validate TOGGLE_CHANGE when the user click on it
     */
    @Test
    fun test04ValidateToggle() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.toggle_button)).perform(
            scrollTo()
        )
        Thread.sleep(500)
        onView(withId(R.id.toggle_button))
            .perform(click())

        Thread.sleep(500)

        val eventType = "\"type\":\"TOGGLE_BUTTON_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateRadioChange - Event: $event")
        Truth.assertThat(event).matches(NID_STRUCT_TOGGLE_CHANGE)
    }

    /**
     * Validate RATING_BAR_CHANGE when the user click on it
     */
    @Test
    fun test05ValidateRatingBar() {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.rating_bar)).perform(
            scrollTo()
        )
        Thread.sleep(500)
        onView(withId(R.id.rating_bar))
            .perform(click())

        Thread.sleep(500)

        val eventType = "\"type\":\"RATING_BAR_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        Truth.assertThat(event).matches(NID_STRUCT_RATING_CHANGE)
    }

    /**
     * Validate SLIDER_CHANGE on NIDOnlyOneFragment class
     */
    @Test
    fun test06ValidateSliderChange() {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        Thread.sleep(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.seekBar_one)).perform(
            scrollTo()
        )

        Thread.sleep(1000)
        getDataStoreInstance().getAllEvents()

        onView(withId(R.id.seekBar_one)).perform(
            swipeRight()
        )

        Thread.sleep(500)

        val eventType = "\"type\":\"SLIDER_CHANGE\""
        val event = validateEventCount(getDataStoreInstance().getAllEvents(), eventType)
        NIDLog.d("----> UITest", "----> validateSliderChange - Event: [$event]")


        Truth.assertThat(event).matches(NID_STRUCT_SLIDER_CHANGE)
    }


    private fun validateEventCount(
        eventList: List<String>,
        eventType: String,
        maxEventsCount: Int = 1
    ): String {
        val events = eventList.filter { it.contains(eventType) }
        if (maxEventsCount > 0) {
            Assert.assertEquals(maxEventsCount, events.size)
        }
        return events.first()
    }

}