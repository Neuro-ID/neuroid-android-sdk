package com.neuroid.tracker

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

internal fun getMockedNeuroID(forceStart:Boolean = false): NeuroID {
    val nidMock = mockk<NeuroID>()

    every { nidMock.forceStart } returns forceStart
    every { nidMock.shouldForceStart() } returns forceStart

    every {
        nidMock.captureEvent(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    } just runs

    return nidMock
}

internal fun getMockedLogger(): NIDLogWrapper {
    val logger = mockk<NIDLogWrapper>()
    every { logger.d(any(), any()) } just runs
    every { logger.i(any(), any()) } just runs
    every { logger.w(any(), any()) } just runs
    every { logger.e(any(), any()) } just runs
    return logger
}

internal fun getMockedRegistrationIdentificationHelper(): RegistrationIdentificationHelper {
    val mocked = mockk<RegistrationIdentificationHelper>()
    every { mocked.registerTargetFromScreen(any(), any(), any(), any(), any()) } just runs
    every { mocked.registerWindowListeners(any()) } just runs
    return mocked
}

internal fun getMockedActivity():Activity{
    val mockedConfiguration = mockk<Configuration>()
    mockedConfiguration.orientation = 0

    val mockedResources = mockk<Resources>()
    every { mockedResources.configuration } returns mockedConfiguration

    val mockedActivity = mockk<Activity>()
    every { mockedActivity.resources } returns mockedResources
    return mockedActivity
}

internal fun verifyCaptureEvent(
    mockedNeuroID: NeuroID,
    eventType:String,
    count:Int = 1,

    // Add params as needed for testing
    attrs:List<Map<String, Any>>? = null,
    o:String? = null
){
    verify(exactly = count) {
        mockedNeuroID.captureEvent(
            any(),
            type=eventType,
            any(),
            attrs=attrs?:any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            o=o?:any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }
}