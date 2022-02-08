package com.neuroid.tracker.callbacks

import android.os.Build
import android.view.Window
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.SearchEvent
import android.view.ActionMode
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.neuroid.tracker.events.NIDTouchEventManager

class NIDWindowCallback(
    windowCall: Window.Callback,
    private val eventManager: NIDTouchEventManager
): Window.Callback {

    private val wrapper = windowCall

    override fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return wrapper.dispatchKeyEvent(keyEvent)
    }

    override fun dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return wrapper.dispatchKeyShortcutEvent(keyEvent)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        eventManager.detectView(motionEvent, System.currentTimeMillis())
        return wrapper.dispatchTouchEvent(motionEvent)
    }

    override fun dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return wrapper.dispatchTrackballEvent(motionEvent)
    }

    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return wrapper.dispatchGenericMotionEvent(motionEvent)
    }

    override fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return wrapper.dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }

    override fun onCreatePanelView(p0: Int): View? {
        return wrapper.onCreatePanelView(p0)
    }

    override fun onCreatePanelMenu(p0: Int, menu: Menu): Boolean {
        return wrapper.onCreatePanelMenu(p0, menu)
    }

    override fun onPreparePanel(p0: Int, view: View?, menu: Menu): Boolean {
        return wrapper.onPreparePanel(p0, view, menu)
    }

    override fun onMenuOpened(p0: Int, menu: Menu): Boolean {
        return wrapper.onMenuOpened(p0, menu)
    }

    override fun onMenuItemSelected(p0: Int, menuItem: MenuItem): Boolean {
        return wrapper.onMenuItemSelected(p0, menuItem)
    }

    override fun onWindowAttributesChanged(layoutParams: WindowManager.LayoutParams?) {
        return wrapper.onWindowAttributesChanged(layoutParams)
    }

    override fun onContentChanged() {
        return wrapper.onContentChanged()
    }

    override fun onWindowFocusChanged(p0: Boolean) {
        return wrapper.onWindowFocusChanged(p0)
    }

    override fun onAttachedToWindow() {
        return wrapper.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return wrapper.onDetachedFromWindow()
    }

    override fun onPanelClosed(p0: Int, menu: Menu) {
        return wrapper.onPanelClosed(p0, menu)
    }

    override fun onSearchRequested(): Boolean {
        return wrapper.onSearchRequested()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return wrapper.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(actionMode: ActionMode.Callback?): ActionMode? {
        return wrapper.onWindowStartingActionMode(actionMode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(p0: ActionMode.Callback?, p1: Int): ActionMode? {
        return wrapper.onWindowStartingActionMode(p0, p1)
    }

    override fun onActionModeStarted(p0: ActionMode?) {
        return wrapper.onActionModeStarted(p0)
    }

    override fun onActionModeFinished(p0: ActionMode?) {
        return wrapper.onActionModeFinished(p0)
    }
}