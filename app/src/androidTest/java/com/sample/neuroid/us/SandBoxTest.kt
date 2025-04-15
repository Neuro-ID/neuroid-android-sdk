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
import androidx.test.platform.app.InstrumentationRegistry
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
import kotlin.time.Duration


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class SandBoxTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            listOf(
                "settings put global window_animation_scale 0",
                "settings put global transition_animation_scale 0",
                "settings put global animator_duration_scale 0"
            ).forEach { command ->
                uiAutomation.executeShellCommand(command).close()
            }
        }
    }
    
    @Before
    fun init() {
        // set dev to scripts and collection endpoint
        NeuroID.getInstance()?.setTestingNeuroIDDevURL()
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
    fun test01RiskyScore() = runTest(timeout = Duration.parse("120s")) {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        val firstNameField = onView(withId(R.id.firstName))
        val lastNameField = onView(withId(R.id.lastName))
        val emailField = onView(withId(R.id.email))
        val cityField = onView(withId(R.id.city))
        val homeZipCodeField = onView(withId(R.id.homeZipCode))
        val phoneNumberField = onView(withId(R.id.phoneNumber))
        val employerlblField = onView(withId(R.id.employerlbl))
        val buttonContinue = onView(withId(R.id.buttonContinue))

        firstNameField.perform(scrollTo())
        firstNameField.perform(click())
        firstNameField.perform(replaceText("Alejandro"))
        firstNameField.perform(closeSoftKeyboard())

        lastNameField.perform(scrollTo())
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Alejandro"))
        lastNameField.perform(closeSoftKeyboard())

        firstNameField.perform(scrollTo())
        firstNameField.perform(clearText())

        lastNameField.perform(scrollTo())
        lastNameField.perform(clearText())

        firstNameField.perform(scrollTo())
        firstNameField.perform(click())
        firstNameField.perform(replaceText("Alejandro"))
        firstNameField.perform(closeSoftKeyboard())

        lastNameField.perform(scrollTo())
        lastNameField.perform(click())
        lastNameField.perform(replaceText("Bautista"))
        lastNameField.perform(closeSoftKeyboard())

        emailField.perform(scrollTo())
        emailField.perform(click())
        emailField.perform(typeText("asdad@gmail.com"))
        emailField.perform(closeSoftKeyboard())

        cityField.perform(scrollTo())
        cityField.perform(click())
        cityField.perform(replaceText("Mexico City"))
        cityField.perform(closeSoftKeyboard())

        homeZipCodeField.perform(scrollTo())
        homeZipCodeField.perform(click())
        homeZipCodeField.perform(replaceText("55340"))
        homeZipCodeField.perform(closeSoftKeyboard())

        phoneNumberField.perform(scrollTo())
        phoneNumberField.perform(click())
        phoneNumberField.perform(replaceText("56565656"))
        phoneNumberField.perform(closeSoftKeyboard())

        employerlblField.perform(scrollTo())
        employerlblField.perform(click())
        employerlblField.perform(replaceText("gasdgsdahgasdgasd"))
        employerlblField.perform(closeSoftKeyboard())

        buttonContinue.perform(scrollTo())
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