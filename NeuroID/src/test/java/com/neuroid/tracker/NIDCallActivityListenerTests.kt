package com.neuroid.tracker

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.events.CallInProgress
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.CallBack
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.VersionChecker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.Executor

class NIDCallActivityListenerTests {

    @Test
    fun test_callActivityListener_call_in_progress_sdk_greater_than_31() {
        callActivityListenerHarness(CallInProgress.ACTIVE.state, CallInProgress.ACTIVE.event, true)
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            true
        )
    }

    @Test
    fun test_callActivityListener_call_in_progress_sdk_lesser_than_31() {
        callActivityListenerHarness(CallInProgress.ACTIVE.state, CallInProgress.ACTIVE.event, false)
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            false
        )
    }

    fun callActivityListenerHarness(callState: Int, isActive: String, sdkGreaterThan31: Boolean) {
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } just runs
        val context = mockk<Context>()
        val telephonyManager = mockk<TelephonyManager>()
        every { context.getSystemService(any()) } returns telephonyManager
        val executor = mockk<Executor>()
        every { context.mainExecutor } returns executor
        val intent = mockk<Intent>()
        val callback = mockk<CallBack>()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThan31() } returns sdkGreaterThan31
        val listener = NIDCallActivityListener(dataStoreManager, version)

        if (sdkGreaterThan31) {
            every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
                mockCallBack(callState, listener)
            }
        } else {
            every { telephonyManager.listen(any(), any()) } answers {
                mockCallBack(callState, listener)
            }
        }

        every { callback.callStateChanged(callState) } just runs

        listener.onReceive(context, intent)

        val callActivityEvent = NIDEventModel(
            type = CALL_IN_PROGRESS, cp = isActive, ts = Calendar.getInstance().timeInMillis
        )

        verify { dataStoreManager.saveEvent((callActivityEvent)) }
        unmockkAll()
    }

    fun mockCallBack(callState: Int, listener: NIDCallActivityListener) {
        when (callState) {
            CallInProgress.INACTIVE.state -> {
                // Mocking saveCallInProgressEvent(0)
                listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)
            }

            CallInProgress.ACTIVE.state -> {
                // Mocking saveCallInProgressEvent(2)
                listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
            }
        }
    }

}