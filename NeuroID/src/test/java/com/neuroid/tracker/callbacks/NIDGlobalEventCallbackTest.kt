package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.TextView
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.events.SingleTargetListenerRegister
import com.neuroid.tracker.events.TouchEventManager
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NIDGlobalEventCallbackTest {
    private lateinit var windowCallback: Window.Callback
    private lateinit var eventManager: TouchEventManager
    private lateinit var viewMainContainer: View
    private lateinit var neuroID: NeuroID
    private lateinit var logger: NIDLogWrapper
    private lateinit var singleTargetListenerRegister: SingleTargetListenerRegister
    private lateinit var callback: NIDGlobalEventCallback

    @Before
    fun setUp() {
        windowCallback = mockk(relaxed = true)
        eventManager = mockk(relaxed = true)
        viewMainContainer = mockk(relaxed = true)
        neuroID = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        singleTargetListenerRegister = mockk(relaxed = true)
        // Clear registeredViews before each test
        NeuroID.registeredViews.clear()
        callback = NIDGlobalEventCallback(
            windowCallback,
            eventManager,
            viewMainContainer,
            neuroID,
            logger,
            singleTargetListenerRegister,
        )
    }

    @After
    fun tearDown() {
        NeuroID.registeredViews.clear()
    }

    // Helper to create a mock EditText with contentDescription so getIdOrTag() returns it
    private fun createMockEditText(idName: String, textValue: String = ""): EditText {
        val editText = mockk<EditText>(relaxed = true)
        every { editText.contentDescription } returns idName
        every { editText.text } returns android.text.SpannableStringBuilder(textValue)
        return editText
    }

    // =============================================
    // OnGlobalFocusChangeListener tests
    // =============================================

    @Test
    fun `onGlobalFocusChanged with EditText newView should fire FOCUS event when already registered`() {
        val editText = createMockEditText("editTextId", "hello")
        // Pre-register so checkOrRegisterTarget takes the "already registered" path
        NeuroID.registeredViews.add("editTextId")

        callback.onGlobalFocusChanged(null, editText)

        verify { neuroID.captureEvent(type = FOCUS, tgs = "editTextId", tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with EditText oldView should fire BLUR event when already registered`() {
        val oldEditText = createMockEditText("oldEditTextId", "world")
        NeuroID.registeredViews.add("oldEditTextId")

        callback.onGlobalFocusChanged(oldEditText, null)

        verify { neuroID.captureEvent(type = BLUR, tgs = "oldEditTextId", tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with both EditText views should fire FOCUS and BLUR events`() {
        val newEditText = createMockEditText("newId", "new")
        val oldEditText = createMockEditText("oldId", "old")
        NeuroID.registeredViews.add("newId")
        NeuroID.registeredViews.add("oldId")

        callback.onGlobalFocusChanged(oldEditText, newEditText)

        verify { neuroID.captureEvent(type = FOCUS, tgs = "newId", tg = any()) }
        verify { neuroID.captureEvent(type = BLUR, tgs = "oldId", tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with unregistered EditText should late-register then fire event`() {
        val editText = createMockEditText("unregisteredId", "text")

        // Mock registerComponent to invoke the onComplete callback (7 params total with defaults)
        every {
            singleTargetListenerRegister.registerComponent(
                any(), any(), any(), any(), any(), any(), any(),
            )
        } answers {
            // The 7th argument (index 6) is the onComplete callback
            (args[6] as () -> Unit).invoke()
        }

        callback.onGlobalFocusChanged(null, editText)

        verify {
            singleTargetListenerRegister.registerComponent(
                eq(neuroID), eq(editText), any(), eq("targetInteractionEvent"), any(), any(), any(),
            )
        }
        verify { neuroID.captureEvent(type = FOCUS, tgs = "unregisteredId", tg = any()) }
        // Verify it was added to registeredViews
        assert(NeuroID.registeredViews.contains("unregisteredId"))
    }

    @Test
    fun `onGlobalFocusChanged with null newView should not fire FOCUS event`() {
        callback.onGlobalFocusChanged(null, null)

        verify(exactly = 0) { neuroID.captureEvent(type = FOCUS, tgs = any(), tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with null oldView should not fire BLUR event`() {
        callback.onGlobalFocusChanged(null, null)

        verify(exactly = 0) { neuroID.captureEvent(type = BLUR, tgs = any(), tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with non-EditText newView should not fire FOCUS event`() {
        val nonEditText = mockk<TextView>(relaxed = true)

        callback.onGlobalFocusChanged(null, nonEditText)

        verify(exactly = 0) { neuroID.captureEvent(type = FOCUS, tgs = any(), tg = any()) }
    }

    @Test
    fun `onGlobalFocusChanged with non-EditText oldView should not fire BLUR event`() {
        val nonEditText = mockk<TextView>(relaxed = true)

        callback.onGlobalFocusChanged(nonEditText, null)

        verify(exactly = 0) { neuroID.captureEvent(type = BLUR, tgs = any(), tg = any()) }
    }

    // =============================================
    // OnGlobalLayoutListener tests
    // =============================================

    @Test
    fun `onGlobalLayout should set initial size and not fire event on first call`() {
        every { viewMainContainer.width } returns 100
        every { viewMainContainer.height } returns 200

        callback.onGlobalLayout()

        verify(exactly = 0) { neuroID.captureEvent(type = WINDOW_RESIZE, w = any(), h = any()) }
    }

    @Test
    fun `onGlobalLayout should fire WINDOW_RESIZE event when size changes`() {
        // First call sets initial size
        every { viewMainContainer.width } returns 100
        every { viewMainContainer.height } returns 200
        callback.onGlobalLayout()

        // Second call with different size triggers resize
        every { viewMainContainer.width } returns 300
        every { viewMainContainer.height } returns 400
        callback.onGlobalLayout()

        verify { neuroID.captureEvent(type = WINDOW_RESIZE, w = 300, h = 400) }
    }

    @Test
    fun `onGlobalLayout should not fire event when size stays the same`() {
        every { viewMainContainer.width } returns 100
        every { viewMainContainer.height } returns 200
        callback.onGlobalLayout()
        callback.onGlobalLayout()

        verify(exactly = 0) { neuroID.captureEvent(type = WINDOW_RESIZE, w = any(), h = any()) }
    }

    @Test
    fun `onGlobalLayout should fire event only when width changes`() {
        every { viewMainContainer.width } returns 100
        every { viewMainContainer.height } returns 200
        callback.onGlobalLayout()

        every { viewMainContainer.width } returns 150
        // height stays 200
        callback.onGlobalLayout()

        verify { neuroID.captureEvent(type = WINDOW_RESIZE, w = 150, h = 200) }
    }

    @Test
    fun `onGlobalLayout should fire event only when height changes`() {
        every { viewMainContainer.width } returns 100
        every { viewMainContainer.height } returns 200
        callback.onGlobalLayout()

        // width stays 100
        every { viewMainContainer.height } returns 250
        callback.onGlobalLayout()

        verify { neuroID.captureEvent(type = WINDOW_RESIZE, w = 100, h = 250) }
    }

    // =============================================
    // Window.Callback delegation tests
    // =============================================

    @Test
    fun `dispatchKeyEvent should delegate to windowCallback`() {
        val keyEvent = mockk<KeyEvent>(relaxed = true)
        callback.dispatchKeyEvent(keyEvent)
        verify { windowCallback.dispatchKeyEvent(keyEvent) }
    }

    @Test
    fun `dispatchKeyShortcutEvent should delegate to windowCallback`() {
        val keyEvent = mockk<KeyEvent>(relaxed = true)
        callback.dispatchKeyShortcutEvent(keyEvent)
        verify { windowCallback.dispatchKeyShortcutEvent(keyEvent) }
    }

    @Test
    fun `dispatchTouchEvent should call eventManager and delegate to windowCallback`() {
        val motionEvent = mockk<MotionEvent>(relaxed = true)
        callback.dispatchTouchEvent(motionEvent)
        verify { eventManager.detectView(motionEvent, any()) }
        verify { windowCallback.dispatchTouchEvent(motionEvent) }
    }

    @Test
    fun `dispatchTrackballEvent should delegate to windowCallback`() {
        val motionEvent = mockk<MotionEvent>(relaxed = true)
        callback.dispatchTrackballEvent(motionEvent)
        verify { windowCallback.dispatchTrackballEvent(motionEvent) }
    }

    @Test
    fun `dispatchGenericMotionEvent should delegate to windowCallback`() {
        val motionEvent = mockk<MotionEvent>(relaxed = true)
        callback.dispatchGenericMotionEvent(motionEvent)
        verify { windowCallback.dispatchGenericMotionEvent(motionEvent) }
    }

    @Test
    fun `dispatchPopulateAccessibilityEvent should delegate to windowCallback`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        callback.dispatchPopulateAccessibilityEvent(event)
        verify { windowCallback.dispatchPopulateAccessibilityEvent(event) }
    }

    @Test
    fun `onCreatePanelView should delegate to windowCallback`() {
        callback.onCreatePanelView(0)
        verify { windowCallback.onCreatePanelView(0) }
    }

    @Test
    fun `onCreatePanelMenu should delegate to windowCallback`() {
        val menu = mockk<Menu>(relaxed = true)
        callback.onCreatePanelMenu(0, menu)
        verify { windowCallback.onCreatePanelMenu(0, menu) }
    }

    @Test
    fun `onPreparePanel should delegate to windowCallback`() {
        val view = mockk<View>(relaxed = true)
        val menu = mockk<Menu>(relaxed = true)
        callback.onPreparePanel(0, view, menu)
        verify { windowCallback.onPreparePanel(0, view, menu) }
    }

    @Test
    fun `onMenuOpened should delegate to windowCallback`() {
        val menu = mockk<Menu>(relaxed = true)
        callback.onMenuOpened(0, menu)
        verify { windowCallback.onMenuOpened(0, menu) }
    }

    @Test
    fun `onMenuItemSelected should delegate to windowCallback`() {
        val menuItem = mockk<MenuItem>(relaxed = true)
        callback.onMenuItemSelected(0, menuItem)
        verify { windowCallback.onMenuItemSelected(0, menuItem) }
    }

    @Test
    fun `onWindowAttributesChanged should delegate to windowCallback`() {
        val layoutParams = mockk<WindowManager.LayoutParams>(relaxed = true)
        callback.onWindowAttributesChanged(layoutParams)
        verify { windowCallback.onWindowAttributesChanged(layoutParams) }
    }

    @Test
    fun `onContentChanged should delegate to windowCallback`() {
        callback.onContentChanged()
        verify { windowCallback.onContentChanged() }
    }

    @Test
    fun `onWindowFocusChanged should delegate to windowCallback`() {
        callback.onWindowFocusChanged(true)
        verify { windowCallback.onWindowFocusChanged(true) }
    }

    @Test
    fun `onAttachedToWindow should delegate to windowCallback`() {
        callback.onAttachedToWindow()
        verify { windowCallback.onAttachedToWindow() }
    }

    @Test
    fun `onDetachedFromWindow should delegate to windowCallback`() {
        callback.onDetachedFromWindow()
        verify { windowCallback.onDetachedFromWindow() }
    }

    @Test
    fun `onPanelClosed should delegate to windowCallback`() {
        val menu = mockk<Menu>(relaxed = true)
        callback.onPanelClosed(0, menu)
        verify { windowCallback.onPanelClosed(0, menu) }
    }

    @Test
    fun `onSearchRequested should delegate to windowCallback`() {
        callback.onSearchRequested()
        verify { windowCallback.onSearchRequested() }
    }

    @Test
    fun `onWindowStartingActionMode with callback should delegate to windowCallback`() {
        val actionModeCallback = mockk<ActionMode.Callback>(relaxed = true)
        callback.onWindowStartingActionMode(actionModeCallback)
        verify { windowCallback.onWindowStartingActionMode(actionModeCallback) }
    }

    @Test
    fun `onActionModeStarted should delegate to windowCallback`() {
        val actionMode = mockk<ActionMode>(relaxed = true)
        callback.onActionModeStarted(actionMode)
        verify { windowCallback.onActionModeStarted(actionMode) }
    }

    @Test
    fun `onActionModeFinished should delegate to windowCallback`() {
        val actionMode = mockk<ActionMode>(relaxed = true)
        callback.onActionModeFinished(actionMode)
        verify { windowCallback.onActionModeFinished(actionMode) }
    }
}
