package com.sample.neuroid.us

import android.util.Log
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.sample.neuroid.us.activities.MainActivity
import com.sample.neuroid.us.activities.NIDOnlyOneFragActivity
import com.sample.neuroid.us.activities.NIDSomeFragmentsActivity
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
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
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
        NeuroID.getInstance().stopSendAllEvents()
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Validate WINDOW_LOAD and WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun validateLifecycleStartAndResume() {
        val statusList = listOf(
            "WINDOW_LOAD",
            "WINDOW_FOCUS"
        )
        Thread.sleep(500) //Wait a half second for create the MainActivity View
        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateLifecycleStartAndResume - Events: [$events]")

        assertThat(statusList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate WINDOW_BLUR and WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun validateLifecyclePauseAndStop() {
        val statusList = listOf(
            "WINDOW_BLUR",
            "WINDOW_UNLOAD"
        )
        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated
        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateLifecyclePauseAndStop - Events: [$events]")

        assertThat(statusList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate TOUCH_START and TOUCH_END when the user click on screen
     */
    @Test
    fun validateClickOnScreen() {
        val eventsList = listOf(
            "TOUCH_START",
            "TOUCH_END"
        )
        onView(withId(R.id.textView_label_one))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateClickOnScreen - Events: [$events]")
        assertThat(eventsList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate TOUCH_MOVE when the user scroll on screen
     */
    @Test
    fun validateSwipeScreen() {
        onView(withId(R.id.layout_main))
            .perform(swipeDown())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateSwipeScreen - Events: [$events]")
        assertThat(events.contains("TOUCH_MOVE")).isTrue()
    }

    /**
     * Validate WINDOW_RESIZE when the user click on editText
     */
    @Test
    fun validateWindowsResize() {
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateWindowsResize - Events: [$events]")
        assertThat(events.contains("WINDOW_RESIZE")).isTrue()
    }

    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun validateFocusOnEditText() {
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateFocusOnEditText - Events: [$events]")
        assertThat(events.contains("FOCUS")).isTrue()
    }

    /**
     * Validate TEXT_CHANGE when the user type on editText
     */
    @Test
    fun validateTypeTextOnEditText() {
        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateTypeTextOnEditText - Events: [$events]")
        assertThat(events.contains("TEXT_CHANGE")).isTrue()
    }

    /**
     * Validate RADIO_CHANGE, CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun validateClickControlViews() {
        val eventsList = listOf(
            "RADIO_CHANGE",
            "CHECKBOX_CHANGE"
        )

        launchActivity<NIDOnlyOneFragActivity>()
        Thread.sleep(500)

        onView(withId(R.id.radioButton_one))
            .perform(click())

        onView(withId(R.id.check_one))
            .perform(click())

        Thread.sleep(1000)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateClickControlViews - Events: [$events]")
        assertThat(eventsList.all { events.contains(it) }).isTrue()
    }

    /**
     * Validate SELECT_CHANGE when the user select one item on list
     */
    @Test
    fun validateComboBoxSelectItem() {

        launchActivity<NIDSomeFragmentsActivity>()
        Thread.sleep(500)

        onView(withId(R.id.spinner_example))
            .perform(click())
        onData(anything()).atPosition(1).perform(click())

        Thread.sleep(1000)

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateComboBoxSelectItem - Events: [$events]")
        assertThat(events.contains("SELECT_CHANGE")).isTrue()
    }

    /**
     * Validate USER_INACTIVE when the user does not interact with the application for 30 seconds
     */
    @Test
    fun validateUserIsInactive() {

        Thread.sleep(31000) // +1 second to wait write data

        val events = getDataStoreInstance().getAllEvents().joinToString(",")
        Log.d("----> UITest", "validateUserIsInactive - Events: [$events]")
        assertThat(events.contains("USER_INACTIVE")).isTrue()
    }

    /**
     * Validate the sending of data to the server correctly, if the return code of the server is
     * NID_OK_SERVICE the sending was successful
     */
    @ExperimentalCoroutinesApi
    @Test
    fun validateSendDataToService() = runBlockingTest {
        //Add some events:
        onView(withId(R.id.editText_normal_field))
            .perform(typeText("Some text"))
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())

        val context = InstrumentationRegistry.getInstrumentation().context
        CoroutineScope(Dispatchers.IO).launch {
            val typeResponse = NIDServiceTracker.sendEventToServer("key_live_vtotrandom_form_mobilesandbox", context)
            assertThat(typeResponse == NIDServiceTracker.NID_OK_SERVICE).isTrue()
        }
    }
}