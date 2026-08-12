package com.neuroid.tracker.callbacks

import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.getMockedConfigService
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ProcessDeviceLifecycleObserverTests {
    private val owner = mockk<LifecycleOwner>()

    @Test
    fun `onStart triggers checkThenCaptureAdvancedDevice when isAdvancedDevice is true`() {
        val neuroID = getMockedNeuroID()
        every { neuroID.isAdvancedDevice } returns true
        every { neuroID.application } returns null

        ProcessDeviceLifecycleObserver(neuroID).onStart(owner)

        verify(exactly = 1) { neuroID.checkThenCaptureAdvancedDevice() }
    }

    @Test
    fun `onStart does not trigger checkThenCaptureAdvancedDevice when isAdvancedDevice is false`() {
        val neuroID = getMockedNeuroID()
        every { neuroID.isAdvancedDevice } returns false
        every { neuroID.application } returns null

        ProcessDeviceLifecycleObserver(neuroID).onStart(owner)

        verify(exactly = 0) { neuroID.checkThenCaptureAdvancedDevice() }
    }

    @Test
    fun `onStart captures a LOG event with the isAdvancedDevice setting`() {
        val neuroID = getMockedNeuroID()
        every { neuroID.isAdvancedDevice } returns true
        every { neuroID.application } returns null

        ProcessDeviceLifecycleObserver(neuroID).onStart(owner)

        verifyCaptureEvent(
            neuroID,
            eventType = LOG,
            m = "isAdvancedDevice setting: true",
            level = "INFO",
        )
    }

    @Test
    fun `onStart triggers captureApplicationMetaData and refreshes the config cache`() {
        val mockedConfigService = getMockedConfigService()
        val neuroID = getMockedNeuroID(mockConfigService = mockedConfigService)
        every { neuroID.isAdvancedDevice } returns false
        every { neuroID.application } returns null

        ProcessDeviceLifecycleObserver(neuroID).onStart(owner)

        verify(exactly = 1) { neuroID.captureApplicationMetaData() }
        verify(exactly = 1) { mockedConfigService.retrieveOrRefreshCache(any()) }
    }
}
