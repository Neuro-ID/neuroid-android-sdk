package com.neuroid.tracker.storage

import android.content.Context
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.service.ConfigService
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration

class NIDDataStoreManagerUnitTests {
    private lateinit var mockContext: Context

    private fun mockDataStore(): NIDDataStoreManagerImp {
        val serviceConfig = mockk<ConfigService>()
        every { serviceConfig.configCache } returns NIDRemoteConfig()
        return NIDDataStoreManagerImp(NIDLogWrapper(), serviceConfig)
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
    fun testQueueEvent() =
        runTest(timeout = Duration.parse("120s")) {
            val dataStore = mockDataStore()
            advanceUntilIdle()

            Assert.assertEquals(0, dataStore.queuedEvents.count())
            dataStore.queueEvent(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                ),
            )

            Assert.assertEquals(1, dataStore.queuedEvents.count())
            dataStore.queuedEvents.clear()
        }

    //    saveAndClearAllQueuedEvents
    @Test
    fun testSaveAndClearAllQueuedEvents() =
        runTest(timeout = Duration.parse("120s")) {
            val dataStore = mockDataStore()
            advanceUntilIdle()

            dataStore.queuedEvents.add(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                ),
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
    fun testSaveEvent() =
        runTest(timeout = Duration.parse("120s")) {
            val dataStore = mockDataStore()
            advanceUntilIdle()

            dataStore.saveEvent(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                ),
            )

            advanceUntilIdle()

            val events = dataStore.getAllEvents()
            Assert.assertEquals(1, events.count())
            Assert.assertEquals(true, events.firstOrNull()?.type === "INPUT")
        }

    //    getAllEvents
    @Test
    fun testGetAllEvents() =
        runTest(timeout = Duration.parse("120s")) {
            val dataStore = mockDataStore()
            advanceUntilIdle()

            dataStore.saveEvent(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                ),
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

    //    clearEvents
    @Test
    fun testClearEvents() =
        runTest(timeout = Duration.parse("120s")) {
            val dataStore = mockDataStore()
            advanceUntilIdle()

            dataStore.saveEvent(
                NIDEventModel(
                    type = INPUT,
                    ts = System.currentTimeMillis(),
                ),
            )

            dataStore.clearEvents()
            advanceUntilIdle()

            val events = dataStore.getAllEvents()
            Assert.assertEquals(0, events.count())
        }

    @Test
    fun isFullBuffer() {
        val dataStore = mockDataStore()
        assert(!dataStore.isFullBuffer())
    }

    @Test
    fun isFullBuffer_isFull() {
        val dataStore = mockDataStore()
        for (i in 0..5000) {
            dataStore.saveEvent(NIDEventModel(INPUT))
        }
        assert(dataStore.isFullBuffer())
    }

    @Test
    fun isFullBuffer_crashMitigation() {
        val serviceConfig = mockk<ConfigService>()
        every { serviceConfig.configCache } returns NIDRemoteConfig()
        val logger = mockk<NIDLogWrapper>()
        every {logger.d(any(), any())} just runs
        val dataStore = NIDDataStoreManagerImp(logger, serviceConfig)
        val raceConditionedEventList = mockk<MutableList<NIDEventModel>>()
        every {raceConditionedEventList.size} returns 0
        every {raceConditionedEventList.isEmpty()} returns false
        every {raceConditionedEventList.last()} throws NoSuchElementException("list is empty fool!")
        every {raceConditionedEventList.add(any())} returns true
        dataStore.eventsList = raceConditionedEventList
        assert(!dataStore.isFullBuffer())
        verify{logger.d(any(), "possible emptying before calling eventsList.last() after empty check occurred list is empty fool!")}
    }
}
