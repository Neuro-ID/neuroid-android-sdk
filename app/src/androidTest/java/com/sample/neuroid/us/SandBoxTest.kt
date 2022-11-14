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
import com.neuroid.tracker.NeuroID
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

    @get:Rule
    var activityRule: ActivityScenarioRule<SandBoxActivity> =
        ActivityScenarioRule(SandBoxActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @Before
    fun stopSendEventsToServer() = runTest {
        //NeuroID.getInstance()?.stop()
        NeuroID.getInstance()?.resetClientId()
    }

    @After
    fun resetDispatchers() = runTest {
        //getDataStoreInstance().clearEvents()
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01RiskyScore() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(12000)
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
        delay(1000)
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        delay(1000)
        firstNameField.perform(clearText())
        delay(1000)
        lastNameField.perform(clearText())
        delay(1000)
        firstNameField.perform(click())
        firstNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        delay(1000)
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Bautista"), closeSoftKeyboard())
        delay(1000)
        emailField.perform(replaceText("asdad@gmail.com"), closeSoftKeyboard())
        delay(1000)
        cityField.perform(typeText("Mexico City"), closeSoftKeyboard())
        delay(1000)
        homeZipCodeField.perform(typeText("55340"), closeSoftKeyboard())
        delay(1000)
        phoneNumberField.perform(typeText("56565656"), closeSoftKeyboard())
        delay(1000)
        employerlblField.perform(typeText("54523"), closeSoftKeyboard())
        delay(15000)
        buttonContinue.perform(click())
        delay(15000)
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