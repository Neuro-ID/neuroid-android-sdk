package com.neuroid.tracker.callbacks

import android.os.Build
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.SingleTargetListenerRegister
import com.neuroid.tracker.events.TEXT_CHANGE
import com.neuroid.tracker.events.TouchEventManager
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson
import com.neuroid.tracker.utils.NIDLogWrapper
import java.util.UUID

class NIDGlobalEventCallback(
    private val windowCallback: Window.Callback,
    private val eventManager: TouchEventManager,
    private val viewMainContainer: View,
    internal val neuroID: NeuroID,
    internal val logger: NIDLogWrapper,
    internal val singleTargetListenerRegister: SingleTargetListenerRegister,
) : ViewTreeObserver.OnGlobalFocusChangeListener,
    ViewTreeObserver.OnGlobalLayoutListener,
    Window.Callback {
    private var lastEditText: EditText? = null
    private var currentWidth = 0
    private var currentHeight = 0

    override fun onGlobalFocusChanged(
        oldView: View?,
        newView: View?,
    ) {
        if (newView != null) {
            if (newView is EditText) {
                registerEditTextViewOnFocusBlur(newView, FOCUS)

                // REMOVING TEXT_CHANGE EVENT for right now
//                lastEditText = if (lastEditText == null) {
//                    newView
//                } else {
//                    lastEditText?.let {
//                        registerTextChangeEvent(it.text.toString())
//                    }
//                    null
//                }
            }
        }

        if (oldView != null) {
            if (oldView is EditText) {
                registerEditTextViewOnFocusBlur(oldView, BLUR)
            }
        }
    }

    override fun onGlobalLayout() {
        if (currentWidth == 0 && currentHeight == 0) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
        }

        if (currentWidth != viewMainContainer.width || currentHeight != viewMainContainer.height) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height

            neuroID.captureEvent(
                type = WINDOW_RESIZE,
                w = currentWidth,
                h = currentHeight,
            )
        }
    }

    private fun registerTextChangeEvent(actualText: String) {
        neuroID.captureEvent(
            type = TEXT_CHANGE,
            tg =
                hashMapOf(
                    "attr" to getAttrJson(actualText),
                    "etn" to lastEditText?.getIdOrTag().orEmpty(),
                    "et" to "text",
                ),
            tgs = lastEditText?.getIdOrTag().orEmpty(),
            sm = 0,
            pd = 0,
            v = "S~C~~${actualText.length}",
            hv = actualText.getSHA256withSalt().take(8),
        )
    }

    // WindowCallback
    override fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return windowCallback.dispatchKeyEvent(keyEvent)
    }

    override fun dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return windowCallback.dispatchKeyShortcutEvent(keyEvent)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        eventManager.detectView(motionEvent, System.currentTimeMillis())
        // REMOVING TEXT_CHANGE EVENT for right now
//        lastEditText?.let {
//            if (lastEditText != view) {
//                registerTextChangeEvent(lastEditText?.text.toString())
//                lastEditText = null
//            }
//        }
        return windowCallback.dispatchTouchEvent(motionEvent)
    }

    override fun dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return windowCallback.dispatchTrackballEvent(motionEvent)
    }

    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return windowCallback.dispatchGenericMotionEvent(motionEvent)
    }

    override fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return windowCallback.dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }

    override fun onCreatePanelView(p0: Int): View? {
        return windowCallback.onCreatePanelView(p0)
    }

    override fun onCreatePanelMenu(
        p0: Int,
        menu: Menu,
    ): Boolean {
        return windowCallback.onCreatePanelMenu(p0, menu)
    }

    override fun onPreparePanel(
        p0: Int,
        view: View?,
        menu: Menu,
    ): Boolean {
        return windowCallback.onPreparePanel(p0, view, menu)
    }

    override fun onMenuOpened(
        p0: Int,
        menu: Menu,
    ): Boolean {
        return windowCallback.onMenuOpened(p0, menu)
    }

    override fun onMenuItemSelected(
        p0: Int,
        menuItem: MenuItem,
    ): Boolean {
        return windowCallback.onMenuItemSelected(p0, menuItem)
    }

    override fun onWindowAttributesChanged(layoutParams: WindowManager.LayoutParams?) {
        return windowCallback.onWindowAttributesChanged(layoutParams)
    }

    override fun onContentChanged() {
        return windowCallback.onContentChanged()
    }

    override fun onWindowFocusChanged(p0: Boolean) {
        return windowCallback.onWindowFocusChanged(p0)
    }

    override fun onAttachedToWindow() {
        return windowCallback.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return windowCallback.onDetachedFromWindow()
    }

    override fun onPanelClosed(
        p0: Int,
        menu: Menu,
    ) {
        return windowCallback.onPanelClosed(p0, menu)
    }

    override fun onSearchRequested(): Boolean {
        return windowCallback.onSearchRequested()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return windowCallback.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(actionMode: ActionMode.Callback?): ActionMode? {
        return windowCallback.onWindowStartingActionMode(actionMode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(
        p0: ActionMode.Callback?,
        p1: Int,
    ): ActionMode? {
        return windowCallback.onWindowStartingActionMode(p0, p1)
    }

    override fun onActionModeStarted(p0: ActionMode?) {
//        val menu = p0?.menu
//        val item = menu?.getItem(0)
//
//
//
//        logger.d(
//            "** ACTION MODE START ${p0.toString()} - ${p0?.title} - ${menu?.size()} - ${item} - ${item?.itemId} - ${p0?.subtitle} - ${p0?.tag}"
//        )
        return windowCallback.onActionModeStarted(p0)
    }

    override fun onActionModeFinished(p0: ActionMode?) {
//        val menu = p0?.menu
//        val item = menu?.getItem(0)
//
//
//        logger.d(
//            "** ACTION MODE FINISH ${p0.toString()} - ${p0?.title} - ${menu?.size()} - ${item} - ${item?.itemId} - ${p0?.subtitle} - ${p0?.tag}"
//        )
        return windowCallback.onActionModeFinished(p0)
    }

    // Helper Functions
    private fun registerEditTextViewOnFocusBlur(
        view: EditText,
        type: String,
    ) {
        val idName = view.getIdOrTag()
        val text = view.text.toString()

        // Do a check to see if we have registered this Field yet
        // used a callback to ensure the registration event happens before the second event
        // if no registration is needed callback fires immediately
        checkOrRegisterTarget(
            view,
        ) {
            neuroID.captureEvent(
                type = type,
                tg =
                    hashMapOf(
                        "attr" to getAttrJson(text),
                    ),
                tgs = idName,
            )
        }
    }

    private fun checkOrRegisterTarget(
        view: EditText,
        onComplete: () -> Unit,
    ) {
        val idName = view.getIdOrTag()
        val simpleJavaClassName = view.javaClass.simpleName

        if (!NeuroID.registeredViews.contains(idName)) {
            logger.d(
                msg = "Late registration: registeringView $simpleJavaClassName",
            )
            val hashCodeAct = view.javaClass.name.hashCode()
            val guid =
                UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

            // Used a callback to ensure the registration event happens before the callback
            singleTargetListenerRegister.registerComponent(
                view,
                guid,
                "targetInteractionEvent",
            ) {
                NeuroID.registeredViews.add(idName)

                onComplete()
            }
        } else {
            logger.d(
                msg = "view already registered: registeringView $simpleJavaClassName tag: $idName",
            )

            onComplete()
        }
    }
}
