package com.neuroid.tracker.events

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDGlobalEventCallback
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegistrationIdentificationHelperTest {
    private lateinit var registrationHelper: RegistrationIdentificationHelper
    private lateinit var neuroID: NeuroID
    private lateinit var logger: NIDLogWrapper

    @Before
    fun setup() {
        logger = mockk<NIDLogWrapper>(relaxed = true)
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        neuroID = getMockedNeuroID()
        registrationHelper = RegistrationIdentificationHelper(logger)
    }

    // -----------------------------------------------------------------------
    // Helper: create a mocked Activity whose window.decorView.findViewById
    // returns the given contentView. Since View.findViewById is final and
    // cannot be mocked, we mock window.decorView to directly return the
    // contentView (the production code casts the result to ViewGroup).
    // -----------------------------------------------------------------------

    private fun createMockedActivityForWindowListeners(
        contentView: ViewGroup = mockk<ViewGroup>(relaxed = true),
        windowCallback: Window.Callback? = mockk<Window.Callback>(relaxed = true),
    ): Triple<Activity, Window, ViewGroup> {
        val mockViewTreeObserver = mockk<ViewTreeObserver>(relaxed = true)
        every { contentView.viewTreeObserver } returns mockViewTreeObserver

        // View.findViewById cannot always be mocked inline.
        // Set it up separately using a relaxed mock.
        val mockDecorView = mockk<View>(relaxed = true)
        every { mockDecorView.findViewById<View>(android.R.id.content) } returns contentView

        val mockWindow = mockk<Window>(relaxed = true)
        every { mockWindow.decorView } returns mockDecorView
        every { mockWindow.callback } returns windowCallback

        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.window } returns mockWindow

        return Triple(mockActivity, mockWindow, contentView)
    }

    // -----------------------------------------------------------------------
    // Initialization Tests
    // -----------------------------------------------------------------------

    @Test
    fun test_registrationIdentificationHelper_initialization() {
        assertEquals(logger, registrationHelper.logger)
        assert(registrationHelper.additionalListeners != null)
        assert(registrationHelper.singleTargetListenerRegister != null)
    }

    // -----------------------------------------------------------------------
    // registerTargetFromScreen Tests
    //
    // Since View.findViewById is final and cannot be mocked with MockK,
    // we use spyk on the helper and stub identifyAllViews to verify that
    // registerTargetFromScreen correctly delegates to it.
    // -----------------------------------------------------------------------

    @Test
    fun test_registerTargetFromScreen_delegatesToIdentifyAllViews() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, contentView) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
        )

        verify {
            spyHelper.identifyAllViews(
                neuroID,
                contentView,
                any(), // guid
                true,  // registerTarget default
                true,  // registerListeners default
                "",    // activityOrFragment default
                "",    // parent default
            )
        }
    }

    @Test
    fun test_registerTargetFromScreen_withRegisterTargetFalse_passesCorrectFlag() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, _) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
            registerTarget = false,
        )

        verify {
            spyHelper.identifyAllViews(
                neuroID,
                any(),
                any(),
                false, // registerTarget
                true,  // registerListeners
                "",
                "",
            )
        }
    }

    @Test
    fun test_registerTargetFromScreen_withRegisterListenersFalse_passesCorrectFlag() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, _) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
            registerListeners = false,
        )

        verify {
            spyHelper.identifyAllViews(
                neuroID,
                any(),
                any(),
                true,  // registerTarget
                false, // registerListeners
                "",
                "",
            )
        }
    }

    @Test
    fun test_registerTargetFromScreen_withActivityOrFragmentAndParent() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, _) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
            activityOrFragment = "TestActivity",
            parent = "TestParent",
        )

        verify {
            spyHelper.identifyAllViews(
                neuroID,
                any(),
                any(),
                true,
                true,
                "TestActivity",
                "TestParent",
            )
        }
    }

    @Test
    fun test_registerTargetFromScreen_withAllParametersFalse() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, _) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
            registerTarget = false,
            registerListeners = false,
            activityOrFragment = "SomeFragment",
            parent = "SomeParent",
        )

        verify {
            spyHelper.identifyAllViews(
                neuroID,
                any(),
                any(),
                false,
                false,
                "SomeFragment",
                "SomeParent",
            )
        }
    }

    @Test
    fun test_registerTargetFromScreen_generatesGuidFromActivityHashCode() {
        val spyHelper = spyk(registrationHelper)
        every {
            spyHelper.identifyAllViews(any(), any(), any(), any(), any(), any(), any())
        } just runs

        val (activity, _, _) = createMockedActivityForWindowListeners()

        spyHelper.registerTargetFromScreen(
            neuroID = neuroID,
            activity = activity,
        )

        // Verify identifyAllViews was called with a non-empty guid
        verify {
            spyHelper.identifyAllViews(
                neuroID,
                any(),
                match { it.isNotEmpty() }, // guid should be a non-empty UUID string
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    // -----------------------------------------------------------------------
    // registerWindowListeners Tests
    // -----------------------------------------------------------------------

    @Test
    fun test_registerWindowListeners_callbackNotNIDGlobalEventCallback_registersNewCallback() {
        val contentView = mockk<ViewGroup>(relaxed = true)

        val regularCallback = mockk<Window.Callback>(relaxed = true)
        val (activity, mockWindow, _) = createMockedActivityForWindowListeners(
            contentView,
            regularCallback,
        )

        // Get the viewTreeObserver that was set up by the helper
        val mockViewTreeObserver = contentView.viewTreeObserver

        registrationHelper.registerWindowListeners(neuroID, activity)

        // Verify the window callback was replaced with NIDGlobalEventCallback
        verify { mockWindow.callback = any<NIDGlobalEventCallback>() }

        // Verify global focus change listener was added
        verify { mockViewTreeObserver.addOnGlobalFocusChangeListener(any()) }

        // Verify global layout listener was added
        verify { mockViewTreeObserver.addOnGlobalLayoutListener(any()) }
    }

    @Test
    fun test_registerWindowListeners_callbackAlreadyNIDGlobalEventCallback_doesNotReRegister() {
        val contentView = mockk<ViewGroup>(relaxed = true)
        val existingNIDCallback = mockk<NIDGlobalEventCallback>(relaxed = true)

        val (activity, mockWindow, _) = createMockedActivityForWindowListeners(
            contentView,
            existingNIDCallback,
        )

        registrationHelper.registerWindowListeners(neuroID, activity)

        // Should NOT set a new callback since it's already an NIDGlobalEventCallback
        verify(exactly = 0) { mockWindow.callback = any() }
    }

    @Test
    fun test_registerWindowListeners_setsCallbackWithCorrectType() {
        val contentView = mockk<ViewGroup>(relaxed = true)

        val regularCallback = mockk<Window.Callback>(relaxed = true)
        val (activity, mockWindow, _) = createMockedActivityForWindowListeners(
            contentView,
            regularCallback,
        )

        // Capture the callback set on the window
        val callbackSlot = slot<Window.Callback>()
        every { mockWindow.callback = capture(callbackSlot) } just runs

        registrationHelper.registerWindowListeners(neuroID, activity)

        assertTrue(callbackSlot.isCaptured)
        assertTrue(callbackSlot.captured is NIDGlobalEventCallback)
    }

    @Test
    fun test_registerWindowListeners_calledTwiceWithSameActivity_secondCallSkips() {
        val contentView = mockk<ViewGroup>(relaxed = true)

        val regularCallback = mockk<Window.Callback>(relaxed = true)
        val (activity, mockWindow, _) = createMockedActivityForWindowListeners(
            contentView,
            regularCallback,
        )

        // First call: regular callback → should register
        val callbackSlot = slot<Window.Callback>()
        every { mockWindow.callback = capture(callbackSlot) } just runs

        registrationHelper.registerWindowListeners(neuroID, activity)

        assertTrue(callbackSlot.isCaptured)

        // Second call: callback is now NIDGlobalEventCallback → should skip
        every { mockWindow.callback } returns callbackSlot.captured

        registrationHelper.registerWindowListeners(neuroID, activity)

        // callback = should have only been set once
        verify(exactly = 1) { mockWindow.callback = any() }
    }

    // -----------------------------------------------------------------------
    // registerSingleTargetListeners Tests — branch coverage
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Helper: create a RegistrationIdentificationHelper with a mocked
    // SingleTargetListenerRegister to verify delegation
    // -----------------------------------------------------------------------

    private fun createHelperWithMockedRegister(): Pair<RegistrationIdentificationHelper, SingleTargetListenerRegister> {
        val helper = RegistrationIdentificationHelper(logger)
        val mockRegister = mockk<SingleTargetListenerRegister>(relaxed = true)

        // Replace the real singleTargetListenerRegister with a mock via reflection
        val field = RegistrationIdentificationHelper::class.java.getDeclaredField("singleTargetListenerRegister")
        field.isAccessible = true
        field.set(helper, mockRegister)

        return Pair(helper, mockRegister)
    }

    @Test
    fun test_registerSingleTargetListeners_withBothFlagsTrue_registersComponentAndListeners() {
        val (helper, mockRegister) = createHelperWithMockedRegister()
        val mockView = mockk<View>(relaxed = true)

        helper.registerSingleTargetListeners(
            neuroID = neuroID,
            view = mockView,
            guid = "test-guid",
            registerTarget = true,
            registerListeners = true,
        )

        verify { mockRegister.registerComponent(neuroID, mockView, "test-guid", any(), any(), any(), any()) }
        verify { mockRegister.registerListeners(neuroID, mockView) }
    }

    @Test
    fun test_registerSingleTargetListeners_withRegisterTargetFalse_skipsComponent() {
        val (helper, mockRegister) = createHelperWithMockedRegister()
        val mockView = mockk<View>(relaxed = true)

        helper.registerSingleTargetListeners(
            neuroID = neuroID,
            view = mockView,
            guid = "test-guid",
            registerTarget = false,
            registerListeners = true,
        )

        verify(exactly = 0) { mockRegister.registerComponent(any(), any(), any(), any(), any(), any(), any()) }
        verify { mockRegister.registerListeners(neuroID, mockView) }
    }

    @Test
    fun test_registerSingleTargetListeners_withRegisterListenersFalse_skipsListeners() {
        val (helper, mockRegister) = createHelperWithMockedRegister()
        val mockView = mockk<View>(relaxed = true)

        helper.registerSingleTargetListeners(
            neuroID = neuroID,
            view = mockView,
            guid = "test-guid",
            registerTarget = true,
            registerListeners = false,
        )

        verify { mockRegister.registerComponent(neuroID, mockView, "test-guid", any(), any(), any(), any()) }
        verify(exactly = 0) { mockRegister.registerListeners(any(), any()) }
    }

    @Test
    fun test_registerSingleTargetListeners_withBothFlagsFalse_skipsBoth() {
        val (helper, mockRegister) = createHelperWithMockedRegister()
        val mockView = mockk<View>(relaxed = true)

        helper.registerSingleTargetListeners(
            neuroID = neuroID,
            view = mockView,
            guid = "test-guid",
            registerTarget = false,
            registerListeners = false,
        )

        verify(exactly = 0) { mockRegister.registerComponent(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { mockRegister.registerListeners(any(), any()) }
    }

    @Test
    fun test_registerSingleTargetListeners_withDefaultParameters() {
        val (helper, mockRegister) = createHelperWithMockedRegister()
        val mockView = mockk<View>(relaxed = true)

        // Call with only required params — exercises $default bridge
        helper.registerSingleTargetListeners(
            neuroID = neuroID,
            view = mockView,
            guid = "test-guid",
        )

        // Defaults: registerTarget=true, registerListeners=true
        verify { mockRegister.registerComponent(any(), any(), any(), any(), activityOrFragment = "", parent = "", any()) }
        verify { mockRegister.registerListeners(neuroID, mockView) }
    }

    // -----------------------------------------------------------------------
    // identifyAllViews Tests — branch coverage for ViewGroup children
    // -----------------------------------------------------------------------

    @Test
    fun test_identifyAllViews_withDefaultParameters() {
        val spyHelper = spyk(registrationHelper)
        val mockViewGroup = mockk<ViewGroup>(relaxed = true)
        every { mockViewGroup.childCount } returns 0

        // Call with only required params — uses default values
        spyHelper.identifyAllViews(
            neuroID = neuroID,
            viewParent = mockViewGroup,
            guid = "test-guid",
        )

        verify { logger.d("NIDDebug identifyAllViews", any()) }
    }

    @Test
    fun test_identifyAllViews_withNonRadioGroupViewGroupChild_recursesButDoesNotRegister() {
        val spyHelper = spyk(registrationHelper)

        // Parent ViewGroup with one child that is a plain ViewGroup (not RadioGroup)
        val childViewGroup = mockk<ViewGroup>(relaxed = true)
        every { childViewGroup.childCount } returns 0

        val parentViewGroup = mockk<ViewGroup>(relaxed = true)
        every { parentViewGroup.childCount } returns 1
        every { parentViewGroup.getChildAt(0) } returns childViewGroup

        every {
            spyHelper.identifySingleView(any(), any(), any(), any(), any(), any(), any())
        } just runs

        spyHelper.identifyAllViews(
            neuroID = neuroID,
            viewParent = parentViewGroup,
            guid = "test-guid",
            registerTarget = true,
            registerListeners = true,
        )

        // Non-RadioGroup ViewGroup should NOT call identifySingleView (shouldRegister stays false)
        verify(exactly = 0) {
            spyHelper.identifySingleView(any(), childViewGroup, any(), any(), any(), any(), any())
        }

        // But it should set an onHierarchyChangeListener on the child ViewGroup
        verify { childViewGroup.setOnHierarchyChangeListener(any()) }
    }

    @Test
    fun test_identifyAllViews_withNonViewGroupChild_registersIt() {
        val spyHelper = spyk(registrationHelper)

        // Parent ViewGroup with one child that is a plain View (not ViewGroup)
        val childView = mockk<View>(relaxed = true)

        val parentViewGroup = mockk<ViewGroup>(relaxed = true)
        every { parentViewGroup.childCount } returns 1
        every { parentViewGroup.getChildAt(0) } returns childView

        every {
            spyHelper.identifySingleView(any(), any(), any(), any(), any(), any(), any())
        } just runs

        spyHelper.identifyAllViews(
            neuroID = neuroID,
            viewParent = parentViewGroup,
            guid = "test-guid",
        )

        // Plain View should call identifySingleView (shouldRegister = true from the else branch)
        verify {
            spyHelper.identifySingleView(neuroID, childView, "test-guid", any(), any(), any(), any())
        }
    }
}
