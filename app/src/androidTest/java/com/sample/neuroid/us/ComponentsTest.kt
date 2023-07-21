package com.sample.neuroid.us

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class ComponentsTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @Before
    fun stopSendEventsToServer() = runTest {
        NIDJobServiceManager.isSendEventsNowEnabled = false
        NeuroID.getInstance()?.stop()
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01ValidateCheckBox() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        onView(withId(R.id.check_one))
            .perform(click())

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }

    /**
     * Validate RADIO_CHANGE when the user click on it
     */
    @Test
    fun test02ValidateRadioChange() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        onView(withId(R.id.radioButton_one))
            .perform(click())

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }

    /**
     * Validate SWITCH_CHANGE when the user click on it
     */
    @Test
    fun test03ValidateSwitch() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        onView(withId(R.id.switch_three)).perform(
            scrollTo()
        )
        delay(500)
        onView(withId(R.id.switch_one))
            .perform(click())

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }

    /**
     * Validate TOGGLE_CHANGE when the user click on it
     */
    @Test
    fun test04ValidateToggle() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        onView(withId(R.id.toggle_button)).perform(
            scrollTo()
        )
        delay(500)
        onView(withId(R.id.toggle_button))
            .perform(click())

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }

    /**
     * Validate RATING_BAR_CHANGE when the user click on it
     */
    @Test
    fun test05ValidateRatingBar() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)

        onView(withId(R.id.rating_bar)).perform(
            scrollTo()
        )
        delay(500)
        onView(withId(R.id.rating_bar))
            .perform(click())

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }

    /**
     * Validate SLIDER_CHANGE on NIDOnlyOneFragment class
     */
    @Test
    fun test06ValidateSliderChange() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) //Wait a half second for create the MainActivity View

        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        delay(500)

        onView(withId(R.id.seekBar_one)).perform(
            scrollTo()
        )

        delay(1000)
        getDataStoreInstance().getAllEvents()

        onView(withId(R.id.seekBar_one)).perform(
            swipeRight()
        )

        delay(500)

        NIDSchema().validateSchema(
            getDataStoreInstance().getAllEvents()
        )
    }


}