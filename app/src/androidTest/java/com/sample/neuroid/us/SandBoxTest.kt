package com.sample.neuroid.us

import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroIDImpl
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.sandbox.SandBoxActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class SandBoxTest {

    @Before
    fun init() {
        // set dev to scripts and collection endpoint
        NeuroIDImpl.getInstance()?.setTestingNeuroIDDevURL()
    }


    @get:Rule
    var activityRule: ActivityScenarioRule<SandBoxActivity> =
        ActivityScenarioRule(SandBoxActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01RiskyScore() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val firstNameField = onView(withId(R.id.firstName))
        val lastNameField = onView(withId(R.id.lastName))
        val emailField = onView(withId(R.id.email))
        val cityField = onView(withId(R.id.city))
        val homeZipCodeField = onView(withId(R.id.homeZipCode))
        val phoneNumberField = onView(withId(R.id.phoneNumber))
        val employerlblField = onView(withId(R.id.employerlbl))
        val buttonContinue = onView(withId(R.id.buttonContinue))
        firstNameField.perform(click())
        firstNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        firstNameField.perform(clearText())
        lastNameField.perform(clearText())
        firstNameField.perform(click())
        firstNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Bautista"), closeSoftKeyboard())
        emailField.perform(replaceText("asdad@gmail.com"), closeSoftKeyboard())
        cityField.perform(typeText("Mexico City"), closeSoftKeyboard())
        homeZipCodeField.perform(typeText("55340"), closeSoftKeyboard())
        phoneNumberField.perform(typeText("56565656"), closeSoftKeyboard())
        employerlblField.perform(typeText("54523"), closeSoftKeyboard())
        buttonContinue.perform(click())
    }


}


class SetEditTextSelectionAction(private val selection: Int) : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
    }

    override fun getDescription(): String {
        return "set selection to $selection"
    }

    override fun perform(uiController: UiController?, view: View?) {
        (view as EditText).selectAll()
    }
}