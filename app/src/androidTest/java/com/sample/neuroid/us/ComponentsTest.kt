package com.sample.neuroid.us

import android.os.Looper
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.time.Duration

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
@Ignore("Ignored until refactor to check specific events")
class ComponentsTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @Before
    fun stopSendEventsToServer() = runTest(timeout = Duration.parse("120s")) {
        // set dev to scripts and collection endpoint
        NeuroID.getInstance()?.setTestingNeuroIDDevURL()

        NeuroID.getInstance()?.isStopped()?.let {
            if (it) {
                NeuroID.getInstance()?.start()
            }
        }
        delay(500)
    }

    @After
    fun resetDispatchers() = runTest(timeout = Duration.parse("120s")) {
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01ValidateCheckBox() = runTest(timeout = Duration.parse("120s")) {
        Looper.prepare()
        NeuroID.getInstance()?.start()

        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        onView(withId(R.id.check_one))
            .perform(click())

        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }

    /**
     * Validate RADIO_CHANGE when the user click on it
     */
    @Test
    fun test02ValidateRadioChange() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        onView(withId(R.id.radioButton_one))
            .perform(click())

        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }

    /**
     * Validate SWITCH_CHANGE when the user click on it
     */
    @Test
    fun test03ValidateSwitch() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        onView(withId(R.id.switch_three)).perform(
            scrollTo()
        )
        onView(withId(R.id.switch_one))
            .perform(click())

        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }

    /**
     * Validate TOGGLE_CHANGE when the user click on it
     */
    @Test
    fun test04ValidateToggle() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        onView(withId(R.id.toggle_button)).perform(
            scrollTo()
        )
        onView(withId(R.id.toggle_button))
            .perform(click())

        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }

    /**
     * Validate RATING_BAR_CHANGE when the user click on it
     */
    @Test
    fun test05ValidateRatingBar() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        onView(withId(R.id.rating_bar)).perform(
            scrollTo()
        )
        onView(withId(R.id.rating_bar))
            .perform(click())


        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }

    /**
     * Validate SLIDER_CHANGE on NIDOnlyOneFragment class
     */
    @Test
    fun test06ValidateSliderChange() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())


        onView(withId(R.id.seekBar_one)).perform(
            scrollTo()
        )

        delay(1000)
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents()

        onView(withId(R.id.seekBar_one)).perform(
            swipeRight()
        )


        NIDSchema().validateSchema(
            NeuroID.getInstance()?.getTestingDataStoreInstance()?.getAllEvents() ?: listOf()
        )
    }


}