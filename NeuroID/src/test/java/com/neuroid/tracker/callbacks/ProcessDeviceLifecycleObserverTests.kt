package com.neuroid.tracker.callbacks

import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.NeuroID
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class ProcessDeviceLifecycleObserverTests {
    @Test
    fun `onStart triggers checkThenCaptureAdvancedDevice on the NeuroID instance`() {
        val neuroID = mockk<NeuroID>()
        every { neuroID.checkThenCaptureAdvancedDevice() } just runs
        every { neuroID.resetClientId() } just runs
        every { neuroID.isAdvancedDevice } returns true

        val owner = mockk<LifecycleOwner>()
        val observer = ProcessDeviceLifecycleObserver(neuroID)

        observer.onStart(owner)

        verify(exactly = 1) { neuroID.checkThenCaptureAdvancedDevice() }
    }
}
