package com.sample.neuroid.us

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
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

/**
 * Neuro ID: 26 UI Test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class NeuroIdUITest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        NeuroID.getInstance()?.stop()
        NIDJobServiceManager.isSendEventsNowEnabled = false
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
     * Validate WINDOW_LOAD on MainActivity class
     */
    @Test
    fun test04ValidateLifecycleStart() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) //Wait a half second for create the MainActivity View
        val eventType = "\"type\":\"WINDOW_LOAD\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_FOCUS on MainActivity class
     */
    @Test
    fun test05ValidateLifecycleResume() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) //Wait a half second for create the MainActivity View

        val eventType = "\"type\":\"WINDOW_FOCUS\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate WINDOW_BLUR on MainActivity class
     */
    @Test
    fun test06ValidateLifecyclePause() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)
        Espresso.pressBack()
        delay(500)
        val eventType = "\"type\":\"WINDOW_BLUR\""
        //TODO Check This event behavior
        NIDSchema().validateEvents(
            getDataStoreInstance().getAllEvents(),
            eventType,
            validateEvent = false
        )
    }

    /**
     * Validate WINDOW_UNLOAD on MainActivity class
     */
    @Test
    fun test07ValidateLifecycleStop() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500)
        onView(withId(R.id.button_show_activity_one_fragment))
            .perform(click())
        delay(500)
        Espresso.pressBack()
        delay(500)

        val eventType = "\"type\":\"WINDOW_UNLOAD\""
        //TODO Check This event behavior
        NIDSchema().validateEvents(
            getDataStoreInstance().getAllEvents(),
            eventType,
            validateEvent = false
        )
    }

    /**
     * Validate TOUCH_START when the user click on screen
     */
    @Test
    fun test08ValidateTouchStart() = runTest {
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
    fun test09ValidateTouchEnd() = runTest {
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
    fun test11ValidateSwipeScreen() = runTest {
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
    fun test12ValidateWindowsResize() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        onView(withId(R.id.editText_normal_field))
            .perform(click())
        delay(1000)

        val eventType = "\"type\":\"WINDOW_RESIZE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, -1)
    }


}