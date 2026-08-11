package com.neuroid.tracker

import android.content.res.Resources
import android.view.View
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.utils.generateUniqueHexID
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class NeuroIdUnitTest {
    @Test
    fun testGenerateUniqueHexId() {
        val uuid = mockk<UUID>()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns uuid

        every { uuid.toString() } returns "test"
        val temp = generateUniqueHexID()
        assertEquals(temp, "test")

        val temp2 = generateUniqueHexID(true)
        assertEquals(temp2, "nid-test")

        unmockkAll()
    }

    @Test
    fun testGetIdOrTag_return_content_description() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every { viewReal.contentDescription } returns "content_desc_test"
        every { viewReal.id } returns 0
        every { viewReal.resources } returns resources
        every { resources.getResourceEntryName(any()) } returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("content_desc_test", value)
    }

    @Test
    fun testGetIdOrTag_return_tag() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every { viewReal.contentDescription } returns ""
        every { viewReal.id } returns -1
        every { viewReal.tag } returns "tag_test"
        every { viewReal.resources } returns resources
        every { resources.getResourceEntryName(any()) } returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("tag_test", value)
    }

    @Test
    fun testGetIdOrTag_return_resources_entry_name() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every { viewReal.contentDescription } returns ""
        every { viewReal.id } returns 10
        every { viewReal.tag } returns "test"
        every { viewReal.resources } returns resources
        every { resources.getResourceEntryName(any()) } returns "REN_test"
        val value = viewReal.getIdOrTag()
        assertEquals("REN_test", value)
    }

    @Test
    fun testGetIdOrTag_return_random_id_contains_id() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every { viewReal.contentDescription } returns ""
        every { viewReal.id } returns 10
        every { viewReal.resources } returns resources
        every { viewReal.x } returns 1000F
        every { viewReal.y } returns 900F
        every { resources.getResourceEntryName(any()) } throws Resources.NotFoundException("")
        val value = viewReal.getIdOrTag()
        assertEquals("View_10000_9000", value)
    }

    @Test
    fun testGetIdOrTag_return_random_id_no_id() {
        val viewReal = mockk<View>()
        val resources = mockk<Resources>()
        every { viewReal.contentDescription } returns ""
        every { viewReal.id } returns -1
        every { viewReal.tag } returns null
        every { viewReal.resources } returns resources
        every { viewReal.x } returns 1000F
        every { viewReal.y } returns 900F
        every { resources.getResourceEntryName(any()) } throws Resources.NotFoundException("")
        val value = viewReal.getIdOrTag()
        assertEquals("View_10000_9000", value)
    }

    @Test
    fun testGetIdOrTag_return_no_id() {
        val viewReal: View? = null
        val value = viewReal.getIdOrTag()
        assertEquals("no_id", value)
    }
}
