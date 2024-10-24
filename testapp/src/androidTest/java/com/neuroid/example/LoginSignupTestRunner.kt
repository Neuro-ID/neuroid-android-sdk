package com.neuroid.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matcher
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(RepeatedTestRunner::class)
class LoginSignupTestRunner {

    // Grant the required permissions for the test
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        val eventCountVariance = 20
        var endSleep = 10000L
        var signupEventCount = 480
        var loginEventCount = 424

        @BeforeClass
        @JvmStatic
        fun disableAnimations() {
            val command = "settings put global transition_animation_scale 0; " +
                    "settings put global window_animation_scale 0; " +
                    "settings put global animator_duration_scale 0;"
            Runtime.getRuntime().exec(command)
        }

        @AfterClass
        @JvmStatic
        fun enableAnimations() {
            val command = "settings put global transition_animation_scale 1; " +
                    "settings put global window_animation_scale 1; " +
                    "settings put global animator_duration_scale 1;"
            Runtime.getRuntime().exec(command)
        }
    }

    var mockWebServer: MockWebServer? = null
    var eventRecorder: EventRecorder? = null

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        eventRecorder = EventRecorder()
        mockWebServer?.let {mockWebServer ->
            mockWebServer.start(8000)
            eventRecorder?.let { eventRecorder ->
                mockWebServer.dispatcher = EventDispatcher(eventRecorder)
            }
        }
    }

    @After
    fun teardown() {
        mockWebServer?.shutdown()
    }

    fun setClipboardText(text: String?) {
        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    class CustomViewActions {
        fun pasteText(): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return ViewMatchers.isAssignableFrom(EditText::class.java)
                }

                override fun getDescription(): String {
                    return "Paste text into the EditText"
                }

                override fun perform(uiController: UiController, view: View) {
                    view.dispatchTouchEvent(
                        MotionEvent.obtain(
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            MotionEvent.ACTION_DOWN,
                            (
                                    view.width / 2).toFloat(),
                            (
                                    view.height / 2).toFloat(),
                            0
                        )
                    )
                    view.dispatchTouchEvent(
                        MotionEvent.obtain(
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            MotionEvent.ACTION_UP,
                            (
                                    view.width / 2).toFloat(),
                            (
                                    view.height / 2).toFloat(),
                            0
                        )
                    )

                    // Simulate paste action
                    (view as EditText).onTextContextMenuItem(android.R.id.paste)
                    uiController.loopMainThreadUntilIdle()
                }
            }
        }
    }

    @Test
    fun runLogin() = runTest(UnconfinedTestDispatcher()) {
        val job = launch {
            //setup session and registered user id
            ApplicationMain.registeredSessionId = RepeatedTestRunner.currentId
            ApplicationMain.registeredUserID = RepeatedTestRunner.currentRegUserId
            RepeatedTestRunner.incrementId()
            RepeatedTestRunner.incrementReguserId()
            println("runLogin: ${ApplicationMain.registeredSessionId}")

            NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()

            ActivityScenario.launch(Splash::class.java)

            // splash page
            Espresso.onView(ViewMatchers.withId(R.id.login))
                .perform(ViewActions.click())

            // login page
            composeTestRule.onNodeWithTag("email").performTextInput(
                "${ApplicationMain.registeredUserName}${ApplicationMain.registeredSessionId}"
            )
            composeTestRule.onNodeWithTag("login_button").performClick()

            // instruction page
            Espresso.onView(ViewMatchers.withId(R.id.start_application_button))
                .perform(ViewActions.click())

            //simulator
            setClipboardText("firstName")
            // Click on the EditText to focus
            Espresso.onView(ViewMatchers.withId(R.id.firstname)).perform(ViewActions.click())
            // Perform the paste action
            Espresso.onView(ViewMatchers.withId(R.id.firstname))
                .perform(CustomViewActions().pasteText())

            Espresso.onView(ViewMatchers.withId(R.id.firstname))
                .perform(ViewActions.typeText("firstname"))
            Espresso.onView(ViewMatchers.withId(R.id.firstname))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.lastname))
                .perform(ViewActions.typeText("lastname"))
            Espresso.onView(ViewMatchers.withId(R.id.lastname))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // loan application
            Espresso.onView(ViewMatchers.withId(R.id.insChip))
                .perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.ptsChip))
                .perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.loan_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // contact information
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(ViewActions.typeText("email@email.com"))
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.phone))
                .perform(ViewActions.typeText("1234567890"))
            Espresso.onView(ViewMatchers.withId(R.id.phone))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // address information
            Espresso.onView(ViewMatchers.withId(R.id.address))
                .perform(ViewActions.typeText("1 washington ave"))
            Espresso.onView(ViewMatchers.withId(R.id.address))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.city))
                .perform(ViewActions.typeText("DC"))
            Espresso.onView(ViewMatchers.withId(R.id.city))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // employer information
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.typeText("test company"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.clearText())
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.typeText("Buy N Large"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.employer_phone))
                .perform(ViewActions.typeText("423423423"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_phone))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.typeText("Somewhere in the universe"))
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.typeText("fix that!"))
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // application summary
            Espresso.onView(ViewMatchers.withId(R.id.home))
                .perform(ViewActions.click())

            // instruction page
            Espresso.onView(ViewMatchers.withId(R.id.send_money_button))
                .perform(ViewActions.click())

            // send money page
            setClipboardText("illegal activities")
            Espresso.onView(ViewMatchers.withId(R.id.description)).perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.description))
                .perform(CustomViewActions().pasteText())
            Espresso.onView(ViewMatchers.withId(R.id.description))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.receiver))
                .perform(ViewActions.typeText("super criminal"))
            Espresso.onView(ViewMatchers.withId(R.id.receiver))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.send_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.send_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.submit_button))
                .perform(ViewActions.click())

            // allow time for the SDK to dump the event queue before terminating (1 minute)
            Thread.sleep(endSleep)
        }
        job.join()

        // verify event count
        eventRecorder?.verifyEventList(loginEventCount, eventCountVariance)
    }

    @Test
    fun runSignup() = runTest(UnconfinedTestDispatcher()) {
        val job = launch {
            //setup session and registered user id
            ApplicationMain.registeredSessionId = RepeatedTestRunner.currentId
            ApplicationMain.registeredUserID = RepeatedTestRunner.currentRegUserId
            RepeatedTestRunner.incrementId()
            RepeatedTestRunner.incrementReguserId()
            println("runSignup: ${ApplicationMain.registeredSessionId}")

            NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()

            ActivityScenario.launch(Splash::class.java)

            // splash page
            Espresso.onView(ViewMatchers.withId(R.id.signup))
                .perform(ViewActions.click())

            // signup page
            // personal information
            setClipboardText("${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
            Espresso.onView(ViewMatchers.withId(R.id.email)).perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(CustomViewActions().pasteText())
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.continue_button))
                .perform(ViewActions.click())

            // personal information two
            Espresso.onView(ViewMatchers.withId(R.id.name))
                .perform(ViewActions.typeText("namesignup"))
            Espresso.onView(ViewMatchers.withId(R.id.name))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.signup_button))
                .perform(ViewActions.click())

            // personal information three
            Espresso.onView(ViewMatchers.withId(R.id.name))
                .perform(ViewActions.typeText("namesignup"))
            Espresso.onView(ViewMatchers.withId(R.id.name))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.signup_button))
                .perform(ViewActions.click())

            // instruction page
            Espresso.onView(ViewMatchers.withId(R.id.start_application_button))
                .perform(ViewActions.click())

            // personal information
            Espresso.onView(ViewMatchers.withId(R.id.firstname))
                .perform(ViewActions.typeText("firstname"))
            Espresso.onView(ViewMatchers.withId(R.id.firstname))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.lastname))
                .perform(ViewActions.typeText("lastname"))
            Espresso.onView(ViewMatchers.withId(R.id.lastname))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // loan application
            Espresso.onView(ViewMatchers.withId(R.id.insChip))
                .perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.ptsChip))
                .perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.loan_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // contact information
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(ViewActions.typeText("email@email.com"))
            Espresso.onView(ViewMatchers.withId(R.id.email))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.phone))
                .perform(ViewActions.typeText("1234567890"))
            Espresso.onView(ViewMatchers.withId(R.id.phone))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())
            //Thread.sleep((Math.random() * 2000).toLong())

            // address information
            Espresso.onView(ViewMatchers.withId(R.id.address))
                .perform(ViewActions.typeText("1 washington ave"))
            Espresso.onView(ViewMatchers.withId(R.id.address))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.city))
                .perform(ViewActions.typeText("DC"))
            Espresso.onView(ViewMatchers.withId(R.id.city))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // employer information
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.typeText("test company"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.clearText())
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.typeText("Buy N Large"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_name))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.employer_phone))
                .perform(ViewActions.typeText("423423423"))
            Espresso.onView(ViewMatchers.withId(R.id.employer_phone))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.typeText("Somewhere in the universe"))
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.typeText("fix that!"))
            Espresso.onView(ViewMatchers.withId(R.id.employerAddress))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.next))
                .perform(ViewActions.click())

            // application summary
            Espresso.onView(ViewMatchers.withId(R.id.home))
                .perform(ViewActions.click())

            // instruction page
            Espresso.onView(ViewMatchers.withId(R.id.send_money_button))
                .perform(ViewActions.click())

            // send money page
            Espresso.onView(ViewMatchers.withId(R.id.description))
                .perform(ViewActions.typeText("illegal activities"))
            Espresso.onView(ViewMatchers.withId(R.id.description))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.receiver))
                .perform(ViewActions.typeText("super criminal"))
            Espresso.onView(ViewMatchers.withId(R.id.receiver))
                .perform(ViewActions.closeSoftKeyboard())
            Espresso.onView(ViewMatchers.withId(R.id.send_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.send_amount_scrubber))
                .perform(SetSeekBarProgress(50))
            Espresso.onView(ViewMatchers.withId(R.id.submit_button))
                .perform(ViewActions.click())

            // allow time for the SDK to dump the event queue before terminating (1 minute)
            Thread.sleep(endSleep)
        }
        job.join()

        // verify event count
        eventRecorder?.verifyEventList(signupEventCount, eventCountVariance)
    }
}