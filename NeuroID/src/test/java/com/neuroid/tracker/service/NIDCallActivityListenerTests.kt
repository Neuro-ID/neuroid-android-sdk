package com.neuroid.tracker.service

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.events.CallInProgress
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.VersionChecker
import com.neuroid.tracker.verifyCaptureEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.Executor

class NIDCallActivityListenerTests {
    @Test
    fun test_callActivityListener_call_in_progress_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.ACTIVE.event,
            true,
            attrs = listOf(mapOf("progress" to "active")))
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            true,
            attrs = listOf(mapOf("progress" to "hangup"))
        )
    }

    @Test
    fun test_callActivityListener_call_ringing_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.RINGING.state,
            "false",
            true,
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    @Test
    fun test_callActivityListener_call_in_progress_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.ACTIVE.event,
            false,
            attrs = listOf(mapOf("progress" to "active"))
        )
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            false,
            attrs = listOf(mapOf("progress" to "hangup"))
        )
    }

    @Test
    fun test_callActivityListener_call_ringing_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.RINGING.state,
            "false",
            false,
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    fun callActivityListenerHarness(
        callState: Int,
        isActive: String,
        sdkGreaterThan31: Boolean,
        attrs: List<Map<String, String>>

    ) {
        val mockedNID = getMockedNeuroID()
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        val context = mockk<Context>()
        val telephonyManager = mockk<TelephonyManager>()
        every { context.getSystemService(any()) } returns telephonyManager
        val executor = mockk<Executor>()
        every { context.mainExecutor } returns executor
        val intent = mockk<Intent>()
        val callback = mockk<CallBack>()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns sdkGreaterThan31
        val listener = NIDCallActivityListener(mockedNID, version)

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

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = isActive,
            attrs = attrs
        )

        unmockkAll()
        unmockkStatic(Calendar::class)
    }

    // Mocking Callback
    fun mockCallBack(
        callState: Int,
        listener: NIDCallActivityListener,
    ) {
        when (callState) {
            CallInProgress.INACTIVE.state -> {
                listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)
            }

            CallInProgress.RINGING.state -> {
                listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
            }

            CallInProgress.ACTIVE.state -> {
                listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
            }
        }
    }
}
