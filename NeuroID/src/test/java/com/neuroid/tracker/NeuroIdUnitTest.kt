package com.neuroid.tracker

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.getIdOrTag
import com.neuroid.tracker.utils.getParentsOfView
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NeuroIdUnitTest {

    @Test
    fun testGetIdOrTag_return_content_description() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every {viewReal.contentDescription} returns "content_desc_test"
        every {viewReal.id} returns 0
        every {viewReal.resources} returns resources
        every {resources.getResourceEntryName(any())} returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("content_desc_test", value)
    }

    @Test
    fun testGetIdOrTag_return_tag() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every {viewReal.contentDescription} returns ""
        every {viewReal.id} returns -1
        every {viewReal.tag} returns "tag_test"
        every {viewReal.resources} returns resources
        every {resources.getResourceEntryName(any())} returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("tag_test", value)
    }

    @Test
    fun testGetIdOrTag_return_resources_entry_name() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every {viewReal.contentDescription} returns ""
        every {viewReal.id} returns 10
        every {viewReal.tag} returns "test"
        every {viewReal.resources} returns resources
        every {resources.getResourceEntryName(any())} returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("REN_test", value)
    }

    @Test
    fun testGetIdOrTag_return_random_id_contains_id() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every {viewReal.contentDescription} returns ""
        every {viewReal.id} returns 10
        every {viewReal.resources} returns resources
        every {viewReal.x} returns 1000F
        every {viewReal.y} returns 900F
        every {resources.getResourceEntryName(any())} throws Resources.NotFoundException("")
        val value = viewReal.getIdOrTag()
        assertEquals("View_10000_9000", value)
    }

    @Test
    fun testGetIdOrTag_return_random_id_no_id() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every {viewReal.contentDescription} returns ""
        every {viewReal.id} returns -1
        every {viewReal.tag} returns null
        every {viewReal.resources} returns resources
        every {viewReal.x} returns 1000F
        every {viewReal.y} returns 900F
        every {resources.getResourceEntryName(any())} throws Resources.NotFoundException("")
        val value = viewReal.getIdOrTag()
        assertEquals("View_10000_9000", value)
    }

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
        assertEquals("ViewGroup${'$'}Subclass1/ViewGroup${'$'}Subclass1/ViewGroup${'$'}Subclass1/", label)
    }
}