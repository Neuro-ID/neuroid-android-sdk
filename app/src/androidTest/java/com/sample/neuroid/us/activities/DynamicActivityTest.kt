package com.sample.neuroid.us.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.NIDSchema
import com.sample.neuroid.us.R
import com.sample.neuroid.us.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@ExperimentalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class DynamicActivityTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<DynamicActivity> =
        ActivityScenarioRule(DynamicActivity::class.java)

    @Before
    fun stopSendEventsToServer() = runTest {
        NeuroID.getInstance()?.stop()
    }

    @After
    fun resetDispatchers() = runTest {
        getDataStoreInstance().clearEvents()
    }

    @Test
    fun test01ValidateFormSubmit() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500)
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.btnAdd))
            .perform(click())
        delay(2000)

        val eventType = "\"type\":\"REGISTER_TARGET\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
        delay(2000)
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.btnAddWithRegisterTarget))
            .perform(click())
        delay(2000)
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
        delay(2000)
    }
}