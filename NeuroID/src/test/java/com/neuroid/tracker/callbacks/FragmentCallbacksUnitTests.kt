package com.neuroid.tracker.callbacks

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.getMockedLogger
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.getMockedRegistrationIdentificationHelper
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class FragmentCallbacksUnitTests {
    //    onFragmentAttached
    @Test
    fun test_onFragmentAttached_no_list()  {
        val mocks = getFragmentCallbackMocks()

        NeuroID.firstScreenName = ""
        NeuroID.screenActivityName = ""
        NeuroID.screenFragName = ""
        NeuroID.screenName = ""

        mocks.fragmentCallbacks.onFragmentAttached(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
            mocks.mockedContext,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        assert(NeuroID.screenName == "AppInit") {
            "value mismatch, received ${NeuroID.screenName}"
        }
        assert(NeuroID.screenFragName == expectedActivityName) {
            "value mismatch, received ${NeuroID.screenFragName}"
        }
        assert(mocks.fragmentCallbacks.listFragment.count() == 1) {
            "listFragment count mismatch"
        }

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentAttached $expectedActivityName",
            )

            mocks.mockedRegistration.registerTargetFromScreen(
                mocks.mockedFragmentActivity,
                true,
                true,
                "fragment",
                parent = expectedActivityName,
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_LOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "fragment",
                        "lifecycle" to "attached",
                        "className" to expectedActivityName,
                    ),
                ),
        )

        mocks.fragmentCallbacks.listFragment.clear()
    }

    @Test
    fun test_onFragmentAttached_existing_list()  {
        val mocks = getFragmentCallbackMocks()

        NeuroID.firstScreenName = ""
        NeuroID.screenActivityName = ""
        NeuroID.screenFragName = ""
        NeuroID.screenName = ""

        val concatName = mocks.mockedFragment.toString().split(" ")
        val fragName =
            if (concatName.isNotEmpty()) {
                concatName[0]
            } else {
                ""
            }
        mocks.fragmentCallbacks.listFragment.clear()
        mocks.fragmentCallbacks.listFragment.add(fragName)

        mocks.fragmentCallbacks.onFragmentAttached(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
            mocks.mockedContext,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        assert(NeuroID.screenName == "AppInit") {
            "value mismatch, received ${NeuroID.screenName}"
        }
        assert(NeuroID.screenFragName == expectedActivityName) {
            "value mismatch, received ${NeuroID.screenFragName}"
        }
        assert(mocks.fragmentCallbacks.listFragment.count() == 1) {
            "listFragment count mismatch actual count ${mocks.fragmentCallbacks.listFragment.count()}"
        }

        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentAttached $expectedActivityName",
            )
        }

        verify(exactly = 0) {
            mocks.mockedRegistration.registerTargetFromScreen(
                mocks.mockedFragmentActivity,
                true,
                true,
                "fragment",
                parent = expectedActivityName,
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_LOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "fragment",
                        "lifecycle" to "attached",
                        "className" to expectedActivityName,
                    ),
                ),
        )

        mocks.fragmentCallbacks.listFragment.remove("Fragment(#13)")
    }

    //    onFragmentCreated
    @Test
    fun test_onFragmentCreated()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentCreated(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
            null,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentViewCreated $expectedActivityName",
            )
        }
    }

    //    onFragmentViewCreated
    @Test
    fun test_onFragmentViewCreated()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentViewCreated(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
            mockk<View>(),
            null,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentViewCreated $expectedActivityName",
            )
        }
    }

    //    onFragmentResumed
    @Test
    fun test_onFragmentResumed_force_start_false()  {
        val mocks = getFragmentCallbackMocks(false)

        mocks.fragmentCallbacks.onFragmentResumed(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Fragment - Resumed ${1} ${false} ${"tag"} $expectedActivityName",
            )

            mocks.mockedLogger.d(
                msg = "Fragment - Resumed - REGISTER TARGET $expectedActivityName",
            )

            mocks.mockedNeuroID.shouldForceStart()

            mocks.mockedRegistration.registerTargetFromScreen(
                mocks.mockedFragmentActivity,
                false,
                true,
                "fragment",
                parent = expectedActivityName,
            )
        }
    }

    @Test
    fun test_onFragmentResumed_force_start_true()  {
        val mocks = getFragmentCallbackMocks(true)

        mocks.fragmentCallbacks.onFragmentResumed(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Fragment - Resumed ${1} ${false} ${"tag"} $expectedActivityName",
            )

            mocks.mockedNeuroID.shouldForceStart()

            mocks.mockedRegistration.registerTargetFromScreen(
                mocks.mockedFragmentActivity,
                true,
                true,
                "fragment",
                parent = expectedActivityName,
            )
        }

        verify(exactly = 0) {
            mocks.mockedLogger.d(
                msg = "Fragment - Resumed - REGISTER TARGET $expectedActivityName",
            )
        }
    }

    //    onFragmentPaused
    @Test
    fun test_onFragmentPaused()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentPaused(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentPaused $expectedActivityName",
            )
        }
    }

    //    onFragmentStopped
    @Test
    fun test_onFragmentStopped()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentStopped(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentStopped $expectedActivityName",
            )
        }
    }

    //    onFragmentDestroyed
    @Test
    fun test_onFragmentDestroyed()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentDestroyed(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "onFragmentDestroyed $expectedActivityName",
            )
        }
    }

    //    onFragmentDetached
    @Test
    fun test_onFragmentDetached()  {
        val mocks = getFragmentCallbackMocks()

        mocks.fragmentCallbacks.onFragmentDetached(
            mocks.mockedFragmentManager,
            mocks.mockedFragment,
        )

        val expectedActivityName = mocks.mockedFragment::class.java.simpleName
        verify(exactly = 1) {
            mocks.mockedLogger.d(
                msg = "Fragment - Detached $expectedActivityName",
            )

            mocks.mockedLogger.d(
                msg = "Fragment - Detached - WINDOW UNLOAD $expectedActivityName",
            )
        }

        verifyCaptureEvent(
            mocks.mockedNeuroID,
            WINDOW_UNLOAD,
            1,
            attrs =
                listOf(
                    mapOf(
                        "component" to "fragment",
                        "lifecycle" to "detached",
                        "className" to expectedActivityName,
                    ),
                ),
        )
    }

    /*
        Mocks and Helper Functions
     */
    data class MockedFragmentCallBackSetup(
        val mockedNeuroID: NeuroID,
        val mockedLogger: NIDLogWrapper,
        val mockedRegistration: RegistrationIdentificationHelper,
        val mockedFragmentManager: FragmentManager,
        val mockedFragmentActivity: FragmentActivity,
        val mockedFragment: Fragment,
        val mockedContext: Context,
        val fragmentCallbacks: FragmentCallbacks,
    )

    fun getFragmentCallbackMocks(NIDForceStart: Boolean = false): MockedFragmentCallBackSetup {
        val mockedNeuroID = getMockedNeuroID(forceStart = NIDForceStart)
        val mockedLogger = getMockedLogger()
        val mockedRegistration = getMockedRegistrationIdentificationHelper()

        val mockedFragmentManager = mockk<FragmentManager>()
        val mockedFragmentActivity = mockk<FragmentActivity>()
        val mockedFragment = mockk<Fragment>()
        every { mockedFragment.requireActivity() } returns mockedFragmentActivity
        every { mockedFragment.id } returns 1
        every { mockedFragment.isVisible } returns false
        every { mockedFragment.tag } returns "tag"

        val mockedContext = mockk<Context>()

        val fragmentCallbacks =
            FragmentCallbacks(
                true,
                neuroID = mockedNeuroID,
                logger = mockedLogger,
                registrationHelper = mockedRegistration,
            )

        return MockedFragmentCallBackSetup(
            mockedNeuroID,
            mockedLogger,
            mockedRegistration,
            mockedFragmentManager,
            mockedFragmentActivity,
            mockedFragment,
            mockedContext,
            fragmentCallbacks,
        )
    }
}
