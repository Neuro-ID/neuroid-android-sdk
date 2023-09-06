package com.neuroid.tracker

import android.content.Context
import android.view.View
import android.view.ViewGroup
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
        every {viewGroup.parent} returns mockk()
        every {viewGroup.id} returns 11
        every {view.parent} returns viewGroup
        val log = mockk<NIDLogWrapper>()
        justRun {log.e(any(), any())}
        val viewReal = View(context)
        val label = viewReal.getParentsOfView(0, view, log)
        assertEquals(true, label.contains("not_a_view"))
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
        assertEquals(true, label.contains("not_a_view"))
    }

    @Test
    fun testGetParentsOfView_deep_root_view_greater_than_3() {
        val context = mockk<Context>()
        val log = mockk<NIDLogWrapper>()
        justRun {log.e(any(), any())}
        val viewGroup1 = mockk<ViewGroup>()
        every { viewGroup1.parent } returns mockk()
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
        assertEquals(false, label.contains("not_a_view"))
    }
}