package com.neuroid.tracker.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
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
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.Executor

class NIDCallActivityListenerTests {
    @Test
    fun test_callActivityListener_call_connected_sdk_greater_than_31() {
        // an "active" call should now update the cp attribute to "connected"
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.CONNECTED.event,
            true,
            attrs = listOf(mapOf("progress" to "active")),
            eventExpected = true,
        )
    }

    @Test
    fun test_callActivityListener_call_disconnected_sdk_greater_than_31() {
        // an "inactive" call should now update the cp attribute to "disconnected"
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.DISCONNECTED.event,
            true,
            attrs = listOf(mapOf("progress" to "hangup")),
            eventExpected = true,
        )
    }

    @Test
    fun test_callActivityListener_call_ringing_does_not_fire_event_sdk_greater_than_31() {
        // ringing detection no longer fires an event, only prints debug output
        callActivityListenerHarness(
            CallInProgress.RINGING.state,
            CallInProgress.RINGING.event,
            true,
            attrs = listOf(mapOf("progress" to "ringing")),
            eventExpected = false,
        )
    }

    @Test
    fun test_callActivityListener_call_connected_sdk_lesser_than_31() {
        // an "active" call should now update the cp attribute to "connected"
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.CONNECTED.event,
            false,
            attrs = listOf(mapOf("progress" to "active")),
            eventExpected = true,
        )
    }

    @Test
    fun test_callActivityListener_call_disconnected_sdk_lesser_than_31() {
        // an "inactive" call should now update the cp attribute to "disconnected"
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.DISCONNECTED.event,
            false,
            attrs = listOf(mapOf("progress" to "hangup")),
            eventExpected = true,
        )
    }

    @Test
    fun test_callActivityListener_call_ringing_does_not_fire_event_sdk_lesser_than_31() {
        // ringing detection no longer fires an event, only prints debug output
        callActivityListenerHarness(
            CallInProgress.RINGING.state,
            CallInProgress.RINGING.event,
            false,
            attrs = listOf(mapOf("progress" to "ringing")),
            eventExpected = false,
        )
    }

    fun callActivityListenerHarness(
        callState: Int,
        expectedCp: String,
        sdkGreaterThan31: Boolean,
        attrs: List<Map<String, String>>,
        eventExpected: Boolean,
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

        if (eventExpected) {
            verifyCaptureEvent(
                mockedNID,
                CALL_IN_PROGRESS,
                cp = expectedCp,
                attrs = attrs
            )
        } else {
            // ringing no longer captures an event, it only logs debug output
            verifyCaptureEvent(
                mockedNID,
                CALL_IN_PROGRESS,
                count = 0,
            )
        }

        unmockkAll()
        unmockkStatic(Calendar::class)
    }

    // Mocking Callback - mirrors the real system call state -> event mapping
    // performed inside NIDCallActivityListener.registerCustomTelephonyCallback
    fun mockCallBack(
        callState: Int,
        listener: NIDCallActivityListener,
    ) {
        when (callState) {
            CallInProgress.INACTIVE.state -> {
                listener.saveCallInProgressEvent(CallInProgress.DISCONNECTED.state)
            }

            CallInProgress.RINGING.state -> {
                listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
            }

            CallInProgress.ACTIVE.state -> {
                listener.saveCallInProgressEvent(CallInProgress.CONNECTED.state)
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ---------------------------------------------------------------------
    // onReceive()
    // ---------------------------------------------------------------------

    @Test
    fun test_onReceive_nullContext_doesNothing() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.onReceive(null, mockk<Intent>())

        verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, count = 0)
    }

    @Test
    fun test_onReceive_calledTwice_onlyRegistersOnce() {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>()
        val telephony = mockk<TelephonyManager>()
        every { context.getSystemService(any()) } returns telephony
        every { context.mainExecutor } returns mockk<Executor>()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        every { telephony.registerTelephonyCallback(any(), any()) } just runs

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())
        // second call is a no-op because the receiver is already registered
        listener.onReceive(context, mockk<Intent>())

        verify(exactly = 1) { telephony.registerTelephonyCallback(any(), any()) }
    }

    // ---------------------------------------------------------------------
    // setCallActivityListener()
    // ---------------------------------------------------------------------

    @Test
    fun test_setCallActivityListener_permissionGranted_registersReceiver() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        val context = mockk<Context>(relaxed = true)

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.setCallActivityListener(context)

        verify { context.registerReceiver(listener, any<IntentFilter>()) }
        verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, count = 0)
    }

    @Test
    fun test_setCallActivityListener_permissionDenied_capturesUnauthorized() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        val context = mockk<Context>(relaxed = true)

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_DENIED

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.setCallActivityListener(context)

        verify(exactly = 0) { context.registerReceiver(any(), any<IntentFilter>()) }
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
            attrs = listOf(mapOf("progress" to "unauthorized")),
        )
    }

    // ---------------------------------------------------------------------
    // unregisterCallActivityListener()
    // ---------------------------------------------------------------------

    @Test
    fun test_unregisterCallActivityListener_registered_sdkGreaterThan31_clearsCallback() {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>(relaxed = true)
        val telephony = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephony
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true

        val listener = NIDCallActivityListener(mockedNID, version)
        // register first so isReceiverRegistered == true
        listener.onReceive(context, mockk<Intent>())

        listener.unregisterCallActivityListener(context)

        verify { context.unregisterReceiver(listener) }
        verify { version.isBuildVersionGreaterThanOrEqualTo31() }
    }

    @Test
    fun test_unregisterCallActivityListener_registered_sdkLesserThan31() {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>(relaxed = true)
        val telephony = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephony
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false

        val listener = NIDCallActivityListener(mockedNID, version)
        // register first so isReceiverRegistered == true
        listener.onReceive(context, mockk<Intent>())

        listener.unregisterCallActivityListener(context)

        verify { context.unregisterReceiver(listener) }
    }

    @Test
    fun test_unregisterCallActivityListener_notRegistered_doesNotUnregister() {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>(relaxed = true)
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.unregisterCallActivityListener(context)

        verify(exactly = 0) { context.unregisterReceiver(any()) }
    }

    @Test
    fun test_unregisterCallActivityListener_nullContext_doesNotCrash() {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>(relaxed = true)
        val telephony = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephony
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true

        val listener = NIDCallActivityListener(mockedNID, version)
        // register so isReceiverRegistered == true, then unregister with null context
        listener.onReceive(context, mockk<Intent>())

        listener.unregisterCallActivityListener(null)

        verify(exactly = 0) { context.unregisterReceiver(any()) }
    }

    // ---------------------------------------------------------------------
    // saveCallInProgressEvent() - remaining branches (UNAUTHORIZED / UNKNOWN)
    // ---------------------------------------------------------------------

    @Test
    fun test_saveCallInProgressEvent_unauthorized_capturesUnauthorized() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.saveCallInProgressEvent(CallInProgress.UNAUTHORIZED.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
            attrs = listOf(mapOf("progress" to "unauthorized")),
        )
    }

    @Test
    fun test_saveCallInProgressEvent_unknown_capturesUnknown() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.saveCallInProgressEvent(CallInProgress.UNKNOWN.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown")),
        )
    }

    // ---------------------------------------------------------------------
    // registerCustomTelephonyCallback() + CustomTelephonyCallback.onCallStateChanged()
    // (API >= 31) — invokes the REAL captured callback to exercise the state mapping
    // ---------------------------------------------------------------------

    @Test
    fun test_customTelephonyCallback_inactive_capturesDisconnected() {
        sdk31RealCallbackHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.DISCONNECTED.event,
            listOf(mapOf("progress" to "hangup")),
            eventExpected = true,
        )
    }

    @Test
    fun test_customTelephonyCallback_active_capturesConnected() {
        sdk31RealCallbackHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.CONNECTED.event,
            listOf(mapOf("progress" to "active")),
            eventExpected = true,
        )
    }

    @Test
    fun test_customTelephonyCallback_ringing_capturesNoEvent() {
        sdk31RealCallbackHarness(
            CallInProgress.RINGING.state,
            null,
            null,
            eventExpected = false,
        )
    }

    @Test
    fun test_customTelephonyCallback_unknownState_capturesUnknown() {
        sdk31RealCallbackHarness(
            777,
            CallInProgress.UNKNOWN.event,
            listOf(mapOf("progress" to "unknown")),
            eventExpected = true,
        )
    }

    private fun sdk31RealCallbackHarness(
        callState: Int,
        expectedCp: String?,
        attrs: List<Map<String, String>>?,
        eventExpected: Boolean,
    ) {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>()
        val telephony = mockk<TelephonyManager>()
        every { context.getSystemService(any()) } returns telephony
        every { context.mainExecutor } returns mockk<Executor>()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true

        val callbackSlot = slot<TelephonyCallback>()
        every { telephony.registerTelephonyCallback(any(), capture(callbackSlot)) } just runs

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())

        // invoke the real callback that was registered to exercise the state -> event mapping
        (callbackSlot.captured as CustomTelephonyCallback).onCallStateChanged(callState)

        if (eventExpected) {
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, cp = expectedCp, attrs = attrs)
        } else {
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, count = 0)
        }
    }

    // ---------------------------------------------------------------------
    // registerCustomTelephonyCallback() + PhoneStateListener.onCallStateChanged()
    // (API < 31) — invokes the REAL captured listener to exercise the state mapping
    // ---------------------------------------------------------------------

    @Test
    fun test_phoneStateListener_idle_capturesDisconnected() {
        sdkLess31RealListenerHarness(
            TelephonyManager.CALL_STATE_IDLE,
            CallInProgress.DISCONNECTED.event,
            listOf(mapOf("progress" to "hangup")),
            eventExpected = true,
        )
    }

    @Test
    fun test_phoneStateListener_offhook_capturesConnected() {
        sdkLess31RealListenerHarness(
            TelephonyManager.CALL_STATE_OFFHOOK,
            CallInProgress.CONNECTED.event,
            listOf(mapOf("progress" to "active")),
            eventExpected = true,
        )
    }

    @Test
    fun test_phoneStateListener_ringing_capturesNoEvent() {
        sdkLess31RealListenerHarness(
            TelephonyManager.CALL_STATE_RINGING,
            null,
            null,
            eventExpected = false,
        )
    }

    @Test
    fun test_phoneStateListener_unknownState_capturesUnknown() {
        sdkLess31RealListenerHarness(
            777,
            CallInProgress.UNKNOWN.event,
            listOf(mapOf("progress" to "unknown")),
            eventExpected = true,
        )
    }

    private fun sdkLess31RealListenerHarness(
        callState: Int,
        expectedCp: String?,
        attrs: List<Map<String, String>>?,
        eventExpected: Boolean,
    ) {
        val mockedNID = getMockedNeuroID()
        val context = mockk<Context>()
        val telephony = mockk<TelephonyManager>()
        every { context.getSystemService(any()) } returns telephony
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false

        val listenerSlot = slot<PhoneStateListener>()
        every { telephony.listen(capture(listenerSlot), any()) } just runs

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())

        // invoke the real listener that was registered to exercise the state -> event mapping
        listenerSlot.captured.onCallStateChanged(callState, null)

        if (eventExpected) {
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, cp = expectedCp, attrs = attrs)
        } else {
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, count = 0)
        }
    }
}
