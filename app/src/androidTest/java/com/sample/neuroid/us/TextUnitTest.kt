package com.sample.neuroid.us

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
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
class TextUnitTest: MockServerTest() {

    // take care of the phone and location permissions dialogs.
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE)

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /*
   Helper Test Functions
    */

    fun forceSendEvents(){
        // stop to force send all events in queue
        NeuroID.getInstance()?.stop()
        delay(500)
    }

    /*
    Actual Tests
     */


    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun test01ValidateFocusOnEditText() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        forceSendEvents()
        assertRequestBodyContains("FOCUS")
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun test02ValidateBlurOnEditText() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.editText_password_field))
            .perform(ViewActions.click())

        forceSendEvents()
        assertRequestBodyContains("BLUR")
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun test03ValidateInputText() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)

        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        delay(500)
        val text = "Some text"
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.typeText(text))

        forceSendEvents()
        assertRequestBodyContains("INPUT")
    }
}