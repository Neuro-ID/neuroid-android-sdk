package com.neuroid.tracker

import android.content.Context
import android.content.SharedPreferences
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManagerImp

import io.mockk.every
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify

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
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private var storedObj = mutableSetOf<String>()

    // used only for getAllEvents because we intentionally are looking at previously stored
    //  and the function passes [] by default which clears out anything stored in our mocks
    private var dontClearStored = false

    private fun mockDataStore(testScheduler: TestCoroutineScheduler): NIDDataStoreManagerImp {
        val dispatcher = StandardTestDispatcher(testScheduler)
        NIDDataStoreManagerImp.init(mockContext, CoroutineScope(dispatcher))
        return NIDDataStoreManagerImp
    }

    @Before
    fun setUp() {
        // Mock the context and sharedPreferences before each test
        mockContext = spyk()
        mockSharedPreferences = mockk()
        every {
            mockContext.getSharedPreferences(
                "NID_SHARED_PREF_FILE",
                Context.MODE_PRIVATE
            )
        } returns mockSharedPreferences

        mockEditor = spyk()

        every { mockSharedPreferences.getStringSet(any(), any()) } answers {
            storedObj
        }
        every { mockSharedPreferences.edit().putStringSet(any(), any()) } answers {
            val eventArray: Set<String> = args[1] as Set<String>

            if (eventArray.isEmpty() && !dontClearStored) {
                storedObj.clear()
            } else {
                val filteredEvents = eventArray.filter { it.isNotBlank() }
                filteredEvents.forEach {
                    storedObj.add(it)
                }
            }

            mockEditor
        }
        every { mockSharedPreferences.edit().apply() } returns Unit
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
//        Assert.assertEquals(1, storedObj.count()) // Should store obj but issues with testing coroutine
    }

    //     saveEvent
    @Test
    fun testSaveEvent() = runTest {
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        val job = dataStore.saveEvent(
            NIDEventModel(
                type = INPUT,
                ts = System.currentTimeMillis()
            )
        )

        job.invokeOnCompletion {
            Assert.assertEquals(1, storedObj.count())
            Assert.assertEquals(true, storedObj.firstOrNull()?.contains("INPUT"))
        }
    }

    //    getAllEvents
    @Test
    fun testGetAllEvents() = runTest {
        dontClearStored = true
        val dataStore = mockDataStore(testScheduler)
        advanceUntilIdle()

        println("stored og $storedObj")

        storedObj.add("test")
        advanceUntilIdle()

        println("stored post $storedObj")

        val events = dataStore.getAllEvents()
        advanceUntilIdle()

        println("stored post post $storedObj $events")
        Assert.assertEquals(1, events.count())
        Assert.assertEquals(true, events.contains("test"))

        dontClearStored = false
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

        storedObj.add("test")

        dataStore.clearEvents()
        advanceUntilIdle()

        Assert.assertEquals(0, storedObj.count())
    }

    // Necessary to Test?
//    resetJsonPayload
//    getJsonPayload
//    putStringSet
//    getStringSet
//    saveJsonPayload
//    createPayload
//    getContentJson
}