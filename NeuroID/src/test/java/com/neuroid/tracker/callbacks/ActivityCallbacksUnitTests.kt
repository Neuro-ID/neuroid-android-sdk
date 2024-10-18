package com.neuroid.tracker.callbacks

import android.app.Activity
import android.os.Bundle
import com.neuroid.tracker.BuildConfig
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_ORIENTATION_CHANGE
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.getMockedActivity
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedRegistrationIdentificationHelper
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class ActivityCallbacksUnitTests {
    //    forceStart
    @Test
    fun test_forceStart()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.forceStart(mocks.mockedActivity)

        verify(exactly = 1) {
            mocks.mockedRegistration.registerTargetFromScreen(
                mocks.mockedActivity,
                true,
                true,
                "activity",
                parent = mocks.mockedActivity::class.java.simpleName,
            )

            mocks.mockedRegistration.registerWindowListeners(mocks.mockedActivity)
        }
    }

    //    onActivityCreated
    @Test
    fun test_onActivityCreated()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivityCreated(mocks.mockedActivity, null)

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onActivityCreated",
            )
        }
    }

    //    onActivityStarted
    @Test
    fun test_onActivityStarted_same_orientation()  {
        val mocks = getActivityCallbackMocks()
        NeuroID.firstScreenName = ""
        NeuroID.screenActivityName = ""
        NeuroID.screenFragName = ""
        NeuroID.screenName = ""

        mocks.activityCallback.onActivityStarted(mocks.mockedActivity)

        val expectedActivityName = mocks.mockedActivity::class.java.name
        assert(NeuroID.firstScreenName == expectedActivityName) {
            "value mistmatch, received ${NeuroID.firstScreenName}"
        }

        assert(NeuroID.screenActivityName == expectedActivityName) {
            "value mismatch, received ${NeuroID.screenActivityName}"
        }

        assert(NeuroID.screenFragName == "") {
            "value mismatch, received ${NeuroID.screenFragName}"
        }

        assert(NeuroID.screenName == "AppInit") {
            "value mismatch, received ${NeuroID.screenName}"
        }

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Created",
            )

            mocks.mockedLogger.d(
                msg = "onActivityStarted existActivity.not()",
            )

            mocks.mockedLogger.d(
                msg = "Activity - POST Created - REGISTER FRAGMENT LIFECYCLES",
            )

            mocks.mockedLogger.d(
                msg = "Activity - POST Created - Window Load",
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_LOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "postCreated",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    @Test
    fun test_onActivityStarted_changed_orientation()  {
        val mocks = getActivityCallbackMocks()
        NeuroID.firstScreenName = ""
        NeuroID.screenActivityName = ""
        NeuroID.screenFragName = ""
        NeuroID.screenName = ""
        mocks.activityCallback.setTestAuxOrientation(2)

        mocks.activityCallback.onActivityStarted(mocks.mockedActivity)

        val expectedActivityName = mocks.mockedActivity::class.java.name
        assert(NeuroID.firstScreenName == expectedActivityName) {
            "value mistmatch, received ${NeuroID.firstScreenName}"
        }

        assert(NeuroID.screenActivityName == expectedActivityName) {
            "value mismatch, received ${NeuroID.screenActivityName}"
        }

        assert(NeuroID.screenFragName == "") {
            "value mismatch, received ${NeuroID.screenFragName}"
        }

        assert(NeuroID.screenName == "AppInit") {
            "value mismatch, received ${NeuroID.screenName}"
        }

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Created",
            )

            mocks.mockedLogger.d(
                msg = "onActivityStarted existActivity.not()",
            )

            mocks.mockedLogger.d(
                msg = "Activity - POST Created - REGISTER FRAGMENT LIFECYCLES",
            )

            mocks.mockedLogger.d(
                msg = "Activity - POST Created - Orientation change",
            )

            mocks.mockedLogger.d(
                msg = "Activity - POST Created - Window Load",
            )
        }

        verifyCaptureEvent(mocks.mockedNeuroID, WINDOW_ORIENTATION_CHANGE, 1, o = "CHANGED")
        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_LOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "postCreated",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    //    onActivityPaused
    @Test
    fun test_onActivityPaused()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivityPaused(mocks.mockedActivity)

        val expectedActivityName = mocks.mockedActivity::class.java.name
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Paused",
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_BLUR,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "paused",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    //    onActivityResumed
    @Test
    fun test_onActivityResumed()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivityResumed(mocks.mockedActivity)

        val expectedActivityName = mocks.mockedActivity::class.java.name
        if (!BuildConfig.FLAVOR.contains("react")) {
            verify(exactly = 1) {
                mocks.mockedLogger.d(
                    msg = "Activity - Resumed",
                )

                mocks.mockedRegistration.registerTargetFromScreen(
                    mocks.mockedActivity,
                    true,
                    true,
                    "activity",
                    parent = expectedActivityName,
                )
                mocks.mockedRegistration.registerWindowListeners(mocks.mockedActivity)
            }
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_FOCUS,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "resumed",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    //    onActivityStopped
    @Test
    fun test_onActivityStopped()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivityStopped(mocks.mockedActivity)

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Stopped",
            )
        }
    }

    //    onActivitySaveInstanceState
    @Test
    fun test_onActivitySaveInstanceState()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivitySaveInstanceState(mocks.mockedActivity, outState = mockk<Bundle>())

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Save Instance",
            )
        }
    }

    //    onActivityDestroyed
    @Test
    fun test_onActivityDestroyed()  {
        val mocks = getActivityCallbackMocks()

        mocks.activityCallback.onActivityDestroyed(mocks.mockedActivity)

        val expectedActivityName = mocks.mockedActivity::class.java.name
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Activity - Destroyed",
            )

            mocks.mockedLogger.d(
                msg = "Activity - Destroyed - Window Unload",
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_UNLOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "activity",
                        "lifecycle" to "destroyed",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    /*
    Mocks and Helper Functions
     */
    data class MockedActivityCallBackSetup(
        val mockedNeuroID: NeuroID,
        val mockedLogger: NIDLogWrapper,
        val mockedRegistration: RegistrationIdentificationHelper,
        val mockedActivity: Activity,
        val activityCallback: ActivityCallbacks,
    )

    fun getActivityCallbackMocks(): MockedActivityCallBackSetup  {
        val mockedNeuroID = getMockedNeuroID()
        val mockedLogger = getMockedLogger()
        val mockedRegistration = getMockedRegistrationIdentificationHelper()
        val mockedActivity = getMockedActivity()

        val activityCallbacks =
            ActivityCallbacks(
                neuroID = mockedNeuroID,
                logger = mockedLogger,
                registrationHelper = mockedRegistration,
            )

        return MockedActivityCallBackSetup(
            mockedNeuroID,
            mockedLogger,
            mockedRegistration,
            mockedActivity,
            activityCallbacks,
        )
    }
}
