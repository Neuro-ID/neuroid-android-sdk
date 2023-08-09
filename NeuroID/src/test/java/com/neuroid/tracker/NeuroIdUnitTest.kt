package com.neuroid.tracker

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.ActionMode
import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.getParentsOfView
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NeuroIdUnitTest {

    /**
     * This test addresses the ENG-5877
     */
    @Test
    fun testGetParentsOfView_root_view_ViewRootImp() {
        val context = mockk<Context>()
        val view = mockk<View>()
        every {view.id} returns 10
        val viewGroup = mockk<ViewGroup>()
        every {viewGroup.parent} returns ViewRootImpl()
        every {viewGroup.id} returns 11
        every {view.parent} returns viewGroup
        val log = mockk<NIDLogWrapper>()
        justRun {log.e(any(), any())}
        val viewReal = View(context)
        val label = viewReal.getParentsOfView(0, view, log)
        assertEquals("ViewGroup${'$'}Subclass1/not_a_view", label)
    }

    @Test
    fun testGetParentsOfView_root_view_null() {
        val context = mockk<Context>()
        val view = mockk<View>()
        every {view.id} returns 10
        val viewGroup = mockk<ViewGroup>()
        every {viewGroup.parent} returns null
        every {viewGroup.id} returns 10
        every {view.parent} returns viewGroup
        val log = mockk<NIDLogWrapper>()
        justRun {log.e(any(), any())}
        val viewReal = View(context)
        val label = viewReal.getParentsOfView(0, view, log)
        assertEquals("ViewGroup${'$'}Subclass1/not_a_view", label)
    }

    @Test
    fun testGetParentsOfView_deep_root_view_greater_than_3() {
        val context = mockk<Context>()
        val log = mockk<NIDLogWrapper>()
        justRun {log.e(any(), any())}
        val viewGroup1 = mockk<ViewGroup>()
        every { viewGroup1.parent } returns ViewRootImpl()
        every { viewGroup1.id } returns 10
        val viewGroup2 = mockk<ViewGroup>()
        every { viewGroup2.parent } returns viewGroup1
        every { viewGroup2.id } returns 11
        val viewGroup3 = mockk<ViewGroup>()
        every { viewGroup3.parent } returns viewGroup2
        every { viewGroup3.id } returns 12
        val viewGroup4 = mockk<ViewGroup>()
        every { viewGroup4.parent } returns viewGroup3
        every { viewGroup4.id } returns 13

        val view = mockk<View>()
        every { view.parent } returns viewGroup4
        every { view.id } returns 14

        val viewReal = View(context)
        val label = viewReal.getParentsOfView(0, view, log)
        assertEquals("ViewGroup${'$'}Subclass1/ViewGroup${'$'}Subclass1/ViewGroup${'$'}Subclass1/", label)
    }

    class ViewRootImpl : ViewParent {
        override fun requestLayout() {
            TODO("Not yet implemented")
        }

        override fun isLayoutRequested(): Boolean {
            TODO("Not yet implemented")
        }

        override fun requestTransparentRegion(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun invalidateChild(p0: View?, p1: Rect?) {
            TODO("Not yet implemented")
        }

        override fun invalidateChildInParent(p0: IntArray?, p1: Rect?): ViewParent {
            TODO("Not yet implemented")
        }

        override fun getParent(): ViewParent {
            TODO("Not yet implemented")
        }

        override fun requestChildFocus(p0: View?, p1: View?) {
            TODO("Not yet implemented")
        }

        override fun recomputeViewAttributes(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun clearChildFocus(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun getChildVisibleRect(p0: View?, p1: Rect?, p2: Point?): Boolean {
            TODO("Not yet implemented")
        }

        override fun focusSearch(p0: View?, p1: Int): View {
            TODO("Not yet implemented")
        }

        override fun keyboardNavigationClusterSearch(p0: View?, p1: Int): View {
            TODO("Not yet implemented")
        }

        override fun bringChildToFront(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun focusableViewAvailable(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun showContextMenuForChild(p0: View?): Boolean {
            TODO("Not yet implemented")
        }

        override fun showContextMenuForChild(p0: View?, p1: Float, p2: Float): Boolean {
            TODO("Not yet implemented")
        }

        override fun createContextMenu(p0: ContextMenu?) {
            TODO("Not yet implemented")
        }

        override fun startActionModeForChild(p0: View?, p1: ActionMode.Callback?): ActionMode {
            TODO("Not yet implemented")
        }

        override fun startActionModeForChild(
            p0: View?,
            p1: ActionMode.Callback?,
            p2: Int
        ): ActionMode {
            TODO("Not yet implemented")
        }

        override fun childDrawableStateChanged(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun requestDisallowInterceptTouchEvent(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun requestChildRectangleOnScreen(p0: View?, p1: Rect?, p2: Boolean): Boolean {
            TODO("Not yet implemented")
        }

        override fun requestSendAccessibilityEvent(
            p0: View?,
            p1: AccessibilityEvent?
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun childHasTransientStateChanged(p0: View?, p1: Boolean) {
            TODO("Not yet implemented")
        }

        override fun requestFitSystemWindows() {
            TODO("Not yet implemented")
        }

        override fun getParentForAccessibility(): ViewParent {
            TODO("Not yet implemented")
        }

        override fun notifySubtreeAccessibilityStateChanged(p0: View?, p1: View, p2: Int) {
            TODO("Not yet implemented")
        }

        override fun canResolveLayoutDirection(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isLayoutDirectionResolved(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getLayoutDirection(): Int {
            TODO("Not yet implemented")
        }

        override fun canResolveTextDirection(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isTextDirectionResolved(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getTextDirection(): Int {
            TODO("Not yet implemented")
        }

        override fun canResolveTextAlignment(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isTextAlignmentResolved(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getTextAlignment(): Int {
            TODO("Not yet implemented")
        }

        override fun onStartNestedScroll(p0: View?, p1: View?, p2: Int): Boolean {
            TODO("Not yet implemented")
        }

        override fun onNestedScrollAccepted(p0: View?, p1: View?, p2: Int) {
            TODO("Not yet implemented")
        }

        override fun onStopNestedScroll(p0: View?) {
            TODO("Not yet implemented")
        }

        override fun onNestedScroll(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int) {
            TODO("Not yet implemented")
        }

        override fun onNestedPreScroll(p0: View?, p1: Int, p2: Int, p3: IntArray?) {
            TODO("Not yet implemented")
        }

        override fun onNestedFling(p0: View?, p1: Float, p2: Float, p3: Boolean): Boolean {
            TODO("Not yet implemented")
        }

        override fun onNestedPreFling(p0: View?, p1: Float, p2: Float): Boolean {
            TODO("Not yet implemented")
        }

        override fun onNestedPrePerformAccessibilityAction(
            p0: View?,
            p1: Int,
            p2: Bundle?
        ): Boolean {
            TODO("Not yet implemented")
        }
    }
}