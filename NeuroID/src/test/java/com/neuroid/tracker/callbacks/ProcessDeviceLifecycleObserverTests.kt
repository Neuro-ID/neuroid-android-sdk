package com.neuroid.tracker.callbacks

import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.getMockedConfigService
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.RootHelper
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProcessDeviceLifecycleObserverTests {
    private val owner = mockk<LifecycleOwner>()

    @After
    fun tearDown() {
        unmockkConstructor(RootHelper::class)
    }

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

    @Test
    fun `onStart creates and assigns NIDMetaData when an application is present`() {
        // RootHelper hits Build fields (e.g. Build.FINGERPRINT) that aren't set on the JVM unit
        // test environment, so its constructor is mocked out here to avoid an NPE.
        mockkConstructor(RootHelper::class)
        every { anyConstructed<RootHelper>().isRooted(any()) } returns false
        every { anyConstructed<RootHelper>().isProbablyEmulator() } returns false

        val mockedContext = mockk<Context>(relaxed = true)
        val mockedApplication = mockk<Application>()
        every { mockedApplication.applicationContext } returns mockedContext

        val neuroID = getMockedNeuroID()
        every { neuroID.isAdvancedDevice } returns false
        every { neuroID.application } returns mockedApplication

        val metaDataSlot = slot<NIDMetaData>()
        every { neuroID.metaData = capture(metaDataSlot) } just runs

        ProcessDeviceLifecycleObserver(neuroID).onStart(owner)

        verify(exactly = 1) { neuroID.metaData = any() }
        assertNotNull(metaDataSlot.captured)
    }
}
