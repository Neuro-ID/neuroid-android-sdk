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
import com.neuroid.tracker.storage.getDataStoreInstance
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

        val firstNameField = onView(withId(R.id.firstName))
        val lastNameNameField = onView(withId(R.id.lastName))
        firstNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        delay(500)
        lastNameNameField.perform(replaceText("Alejandro"), closeSoftKeyboard())
        delay(500)
        firstNameField.perform(clearText())
        delay(500)
        lastNameNameField.perform(clearText())
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