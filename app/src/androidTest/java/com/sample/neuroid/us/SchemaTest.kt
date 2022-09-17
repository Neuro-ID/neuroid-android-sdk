package com.sample.neuroid.us

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
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
class SchemaTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

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
    fun test01ValidateSchema() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        delay(500) // When you go to the next test, the activity is destroyed and recreated

        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_one_fragment))
            .perform(ViewActions.click())
        delay(500)

        Espresso.onView(ViewMatchers.withId(R.id.check_one))
            .perform(ViewActions.click())

        delay(500)

        val events = getDataStoreInstance().getAllEvents()

        NIDSchema().validateEvents(events)
    }

}
