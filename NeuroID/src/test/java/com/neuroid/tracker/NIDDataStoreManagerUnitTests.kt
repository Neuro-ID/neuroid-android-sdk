package com.neuroid.tracker

import android.content.Context
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManagerImp

import io.mockk.spyk
import io.mockk.unmockkAll

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class NIDDataStoreManagerUnitTests {

    private lateinit var mockContext: Context

    private fun mockDataStore(testScheduler: TestCoroutineScheduler): NIDDataStoreManagerImp {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return NIDDataStoreManagerImp(CoroutineScope(dispatcher))
    }

    @Before
    fun setUp() {
        mockContext = spyk()
    }

    @After
    fun tearDown() {
        // Clean up mocks after each test
        unmockkAll()
    }

    //    queueEvent
    @Test
    fun testQueueEvent() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        Assert.assertEquals(0, dataStore.queuedEvents.count())
        dataStore.queueEvent(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )

        Assert.assertEquals(1, dataStore.queuedEvents.count())
        dataStore.queuedEvents.clear()
    }

    //    saveAndClearAllQueuedEvents
    @Test
    fun testSaveAndClearAllQueuedEvents() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        dataStore.queuedEvents.add(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )
        Assert.assertEquals(1, dataStore.queuedEvents.count())


        dataStore.saveAndClearAllQueuedEvents()
        advanceUntilIdle()

        Assert.assertEquals(0, dataStore.queuedEvents.count())

        val events = dataStore.getAllEvents()
        Assert.assertEquals(1, events.count())
    }

    //     saveEvent
    @Test
    fun testSaveEvent() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

      dataStore.saveEvent(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )

        advanceUntilIdle()

        val events = dataStore.getAllEvents()
        Assert.assertEquals(1, events.count())
        Assert.assertEquals(true, events.firstOrNull()?.type === "INPUT")
    }

    //    getAllEvents
    @Test
    fun testGetAllEvents() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        dataStore.saveEvent(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )
        advanceUntilIdle()

        var events = dataStore.getAllEvents()
        advanceUntilIdle()

        Assert.assertEquals(1, events.count())
        Assert.assertEquals(true, events.firstOrNull()?.type === "INPUT")

        advanceUntilIdle()

        events = dataStore.getAllEvents()
        Assert.assertEquals(0, events.count())
    }

    //    addViewIdExclude
    @Test
    fun testAddViewIdExclude_single() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        dataStore.addViewIdExclude("excludeMe")
        Assert.assertEquals(1, dataStore.listIdsExcluded.count())
        Assert.assertEquals("excludeMe", dataStore.listIdsExcluded[0])
    }

    @Test
    fun testAddViewIdExclude_double() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        dataStore.addViewIdExclude("excludeMe")
        Assert.assertEquals(1, dataStore.listIdsExcluded.count())
        Assert.assertEquals("excludeMe", dataStore.listIdsExcluded[0])

        dataStore.addViewIdExclude("excludeMe")
        Assert.assertEquals(1, dataStore.listIdsExcluded.count())
    }

    //    clearEvents
    @Test
    fun testClearEvents() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        dataStore.saveEvent(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )

        dataStore.clearEvents()
        advanceUntilIdle()

        val events = dataStore.getAllEvents()
        Assert.assertEquals(0, events.count())
    }
}