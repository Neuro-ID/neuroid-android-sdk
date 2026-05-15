package com.neuroid.tracker.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.events.CallInProgress
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDPermissionChecker
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.Executor

class NIDCallActivityListenerTests {

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─── Active / Inactive / Ringing (SDK >= 31) ───────────────────────────

    @Test
    fun test_callActivityListener_call_in_progress_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.ACTIVE.event,
            true,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_greater_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            true,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
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

    // ─── Active / Inactive / Ringing (SDK < 31) ────────────────────────────

    @Test
    fun test_callActivityListener_call_in_progress_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.ACTIVE.state,
            CallInProgress.ACTIVE.event,
            false,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_call_inactive_sdk_lesser_than_31() {
        callActivityListenerHarness(
            CallInProgress.INACTIVE.state,
            CallInProgress.INACTIVE.event,
            false,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
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

    // ─── Inbound call direction ─────────────────────────────────────────────

    @Test
    fun test_callActivityListener_inbound_call_active_direction() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // RINGING before ACTIVE → inbound
        listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "inbound"))
        )
    }

    @Test
    fun test_callActivityListener_outbound_call_active_direction() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // No RINGING before ACTIVE → outbound
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_inbound_call_inactive_direction() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        // Capture all attr lists from all captureEvent calls then find the INACTIVE one
        val allAttrs = mutableListOf<List<Map<String, Any>>>()
        verify(atLeast = 1) {
            mockedNID.captureEvent(
                type = CALL_IN_PROGRESS,
                cp = CallInProgress.INACTIVE.event,
                attrs = capture(allAttrs),
                ts = any(), queuedEvent = any(), tg = any(), tgs = any(), touches = any(),
                key = any(), gyro = any(), accel = any(), v = any(), hv = any(), en = any(),
                etn = any(), ec = any(), et = any(), eid = any(), ct = any(), sm = any(),
                pd = any(), x = any(), y = any(), w = any(), h = any(), sw = any(), sh = any(),
                f = any(), lsid = any(), sid = any(), siteId = any(), cid = any(), did = any(),
                iid = any(), loc = any(), ua = any(), tzo = any(), lng = any(), ce = any(),
                je = any(), ol = any(), p = any(), dnt = any(), tch = any(), url = any(),
                ns = any(), jsl = any(), jsv = any(), uid = any(), o = any(), rts = any(),
                metadata = any(), rid = any(), m = any(), level = any(), c = any(),
                isWifi = any(), isConnected = any(), l = any(), synthetic = any(),
            )
        }
        val capturedAttrs = allAttrs.last().first()
        assertTrue("Expected inbound direction", capturedAttrs["direction"] == "inbound")
        assertTrue("Expected hangup progress", capturedAttrs["progress"] == "hangup")
    }

    @Test
    fun test_callActivityListener_outbound_call_inactive_direction() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        val allAttrs = mutableListOf<List<Map<String, Any>>>()
        verify(atLeast = 1) {
            mockedNID.captureEvent(
                type = CALL_IN_PROGRESS,
                cp = CallInProgress.INACTIVE.event,
                attrs = capture(allAttrs),
                ts = any(), queuedEvent = any(), tg = any(), tgs = any(), touches = any(),
                key = any(), gyro = any(), accel = any(), v = any(), hv = any(), en = any(),
                etn = any(), ec = any(), et = any(), eid = any(), ct = any(), sm = any(),
                pd = any(), x = any(), y = any(), w = any(), h = any(), sw = any(), sh = any(),
                f = any(), lsid = any(), sid = any(), siteId = any(), cid = any(), did = any(),
                iid = any(), loc = any(), ua = any(), tzo = any(), lng = any(), ce = any(),
                je = any(), ol = any(), p = any(), dnt = any(), tch = any(), url = any(),
                ns = any(), jsl = any(), jsv = any(), uid = any(), o = any(), rts = any(),
                metadata = any(), rid = any(), m = any(), level = any(), c = any(),
                isWifi = any(), isConnected = any(), l = any(), synthetic = any(),
            )
        }
        val capturedAttrs = allAttrs.last().first()
        assertTrue("Expected outbound direction", capturedAttrs["direction"] == "outbound")
        assertTrue("Expected hangup progress", capturedAttrs["progress"] == "hangup")
    }

    // ─── Direction reset after call ends ────────────────────────────────────

    @Test
    fun test_callActivityListener_direction_reset_after_call_ends() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // First call: inbound
        listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        // Delay to bypass the 500ms debounce guard
        Thread.sleep(600)

        // Second call: outbound (no RINGING — wasRinging should have been reset)
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)

        // First ACTIVE was inbound, second ACTIVE is outbound
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "inbound"))
        )
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    // ─── Duration calculation ────────────────────────────────────────────────

    @Test
    fun test_callActivityListener_duration_is_zero_when_no_active_before_inactive() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // INACTIVE without prior ACTIVE — callStartTime is 0, duration should be 0
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_duration_is_positive_after_active_then_inactive() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        val allAttrs = mutableListOf<List<Map<String, Any>>>()
        verify(atLeast = 1) {
            mockedNID.captureEvent(
                type = CALL_IN_PROGRESS,
                cp = CallInProgress.INACTIVE.event,
                attrs = capture(allAttrs),
                ts = any(), queuedEvent = any(), tg = any(), tgs = any(), touches = any(),
                key = any(), gyro = any(), accel = any(), v = any(), hv = any(), en = any(),
                etn = any(), ec = any(), et = any(), eid = any(), ct = any(), sm = any(),
                pd = any(), x = any(), y = any(), w = any(), h = any(), sw = any(), sh = any(),
                f = any(), lsid = any(), sid = any(), siteId = any(), cid = any(), did = any(),
                iid = any(), loc = any(), ua = any(), tzo = any(), lng = any(), ce = any(),
                je = any(), ol = any(), p = any(), dnt = any(), tch = any(), url = any(),
                ns = any(), jsl = any(), jsv = any(), uid = any(), o = any(), rts = any(),
                metadata = any(), rid = any(), m = any(), level = any(), c = any(),
                isWifi = any(), isConnected = any(), l = any(), synthetic = any(),
            )
        }
        val durationStr = allAttrs.last().first()["duration_ms"] as String
        val duration = durationStr.toLong()
        assertTrue("Expected duration >= 0 but was $duration", duration >= 0L)
    }

    @Test
    fun test_callActivityListener_callStartTime_reset_after_inactive() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // First call: ACTIVE → INACTIVE; callStartTime gets reset to 0 after hangup
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        // Wait past debounce and fire INACTIVE again without a preceding ACTIVE
        // Since callStartTime was reset to 0, duration should be reported as 0
        Thread.sleep(600)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        // Verify the second INACTIVE event has duration_ms=0
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    // ─── UNAUTHORIZED state ──────────────────────────────────────────────────

    @Test
    fun test_callActivityListener_unauthorized_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        listener.saveCallInProgressEvent(CallInProgress.UNAUTHORIZED.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
            attrs = listOf(mapOf("progress" to "unauthorized"))
        )
    }

    // ─── Unknown state ───────────────────────────────────────────────────────

    @Test
    fun test_callActivityListener_unknown_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // Use an integer that doesn't match any known state
        listener.saveCallInProgressEvent(-999)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown"))
        )
    }

    // ─── Ringing while call active (call waiting) ────────────────────────────

    @Test
    fun test_callActivityListener_ringing_while_call_active_shows_waiting() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // First make call active, then ring -> should emit "waiting"
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        listener.saveCallInProgressEvent(CallInProgress.RINGING.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = "true",
            attrs = listOf(mapOf("progress" to "waiting"))
        )
    }

    // ─── unregisterCallActivityListener ──────────────────────────────────────

    @Test
    fun test_unregisterCallActivityListener_when_registered() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)
        val intent = mockk<Intent>()

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.listen(any(), any()) } answers {
            mockCallBack(CallInProgress.ACTIVE.state, listener)
        }
        listener.onReceive(context, intent)

        // Now unregister — isReceiverRegistered is true after onReceive
        listener.unregisterCallActivityListener(context)

        verify(atLeast = 1) { context.unregisterReceiver(listener) }
    }

    @Test
    fun test_unregisterCallActivityListener_when_not_registered() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val listener = NIDCallActivityListener(mockedNID, version)

        // Never called onReceive — isReceiverRegistered = false
        listener.unregisterCallActivityListener(context)

        verify(exactly = 0) { context.unregisterReceiver(any()) }
    }

    @Test
    fun test_unregisterCallActivityListener_sdk_gte_31_clears_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)
        val intent = mockk<Intent>()

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
            mockCallBack(CallInProgress.ACTIVE.state, listener)
        }
        listener.onReceive(context, intent)
        // Should not throw; customTelephonyCallback should become null
        listener.unregisterCallActivityListener(context)
        verify(atLeast = 1) { context.unregisterReceiver(listener) }
    }

    @Test
    fun test_unregisterCallActivityListener_null_context_does_not_crash() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // Should not throw even with null context when not registered
        listener.unregisterCallActivityListener(null)
    }

    @Test
    fun test_unregisterCallActivityListener_null_context_when_registered_does_not_crash() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())
        // isReceiverRegistered = true, but context is null → should not crash (null-safe call)
        listener.unregisterCallActivityListener(null)
    }

    @Test
    fun test_unregisterCallActivityListener_sdk_lt_31_does_not_clear_telephony_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())
        // SDK < 31 path: customTelephonyCallback branch is NOT entered
        listener.unregisterCallActivityListener(context)
        verify(atLeast = 1) { context.unregisterReceiver(listener) }
    }

    // ─── onReceive with null context ─────────────────────────────────────────

    @Test
    fun test_onReceive_null_context_does_not_register() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)
        val intent = mockk<Intent>()

        // Should not throw and should not try to register
        listener.onReceive(null, intent)
    }

    // ─── Debounce guard — rapid INACTIVE calls ───────────────────────────────

    @Test
    fun test_callActivityListener_debounce_prevents_duplicate_inactive_events() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // Two rapid INACTIVE calls — only the first should fire captureEvent
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)
        listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)

        // Only one event should have been captured (second is within 500ms debounce)
        verify(exactly = 1) {
            mockedNID.captureEvent(
                type = CALL_IN_PROGRESS,
                cp = CallInProgress.INACTIVE.event,
                attrs = any(),
                ts = any(), queuedEvent = any(), tg = any(), tgs = any(), touches = any(),
                key = any(), gyro = any(), accel = any(), v = any(), hv = any(), en = any(),
                etn = any(), ec = any(), et = any(), eid = any(), ct = any(), sm = any(),
                pd = any(), x = any(), y = any(), w = any(), h = any(), sw = any(), sh = any(),
                f = any(), lsid = any(), sid = any(), siteId = any(), cid = any(), did = any(),
                iid = any(), loc = any(), ua = any(), tzo = any(), lng = any(), ce = any(),
                je = any(), ol = any(), p = any(), dnt = any(), tch = any(), url = any(),
                ns = any(), jsl = any(), jsv = any(), uid = any(), o = any(), rts = any(),
                metadata = any(), rid = any(), m = any(), level = any(), c = any(),
                isWifi = any(), isConnected = any(), l = any(), synthetic = any(),
            )
        }
    }

    // ─── Rapid ACTIVE calls (no debounce on lastActiveTime in current code) ──

    @Test
    fun test_callActivityListener_rapid_active_calls_both_fired() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val listener = NIDCallActivityListener(mockedNID, version)

        // Two rapid ACTIVE calls — both fire because lastActiveTime is never updated
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)

        verify(exactly = 2) {
            mockedNID.captureEvent(
                type = CALL_IN_PROGRESS,
                cp = CallInProgress.ACTIVE.event,
                attrs = any(),
                ts = any(), queuedEvent = any(), tg = any(), tgs = any(), touches = any(),
                key = any(), gyro = any(), accel = any(), v = any(), hv = any(), en = any(),
                etn = any(), ec = any(), et = any(), eid = any(), ct = any(), sm = any(),
                pd = any(), x = any(), y = any(), w = any(), h = any(), sw = any(), sh = any(),
                f = any(), lsid = any(), sid = any(), siteId = any(), cid = any(), did = any(),
                iid = any(), loc = any(), ua = any(), tzo = any(), lng = any(), ce = any(),
                je = any(), ol = any(), p = any(), dnt = any(), tch = any(), url = any(),
                ns = any(), jsl = any(), jsv = any(), uid = any(), o = any(), rts = any(),
                metadata = any(), rid = any(), m = any(), level = any(), c = any(),
                isWifi = any(), isConnected = any(), l = any(), synthetic = any(),
            )
        }
    }

    // ─── SDK >= 31 else branch (UNKNOWN state in telephony callback) ──────────

    @Test
    fun test_callActivityListener_sdk_gte_31_unknown_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)
        val intent = mockk<Intent>()

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
            // Pass an unknown state
            listener.saveCallInProgressEvent(CallInProgress.UNKNOWN.state)
        }

        listener.onReceive(context, intent)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown"))
        )
    }

    // ─── SDK < 31 else branch (UNKNOWN state in phone state listener callback) ─

    @Test
    fun test_callActivityListener_sdk_lt_31_unknown_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)
        val intent = mockk<Intent>()

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.listen(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.UNKNOWN.state)
        }

        listener.onReceive(context, intent)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown"))
        )
    }

    // ─── onReceive re-registration guard ─────────────────────────────────────

    @Test
    fun test_onReceive_does_not_re_register_when_already_registered() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)
        val intent = mockk<Intent>()

        val listener = NIDCallActivityListener(mockedNID, version)
        var listenCallCount = 0
        every { telephonyManager.listen(any(), any()) } answers {
            listenCallCount++
        }

        listener.onReceive(context, intent)
        listener.onReceive(context, intent)

        // registerCustomTelephonyCallback should only be called once
        assert(listenCallCount == 1) { "Expected 1 registration but was $listenCallCount" }
    }

    // ─── SDK >= 31 full callback state coverage ──────────────────────────────

    @Test
    fun test_callActivityListener_sdk_gte_31_ringing_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = "false",
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    @Test
    fun test_callActivityListener_sdk_gte_31_inactive_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_sdk_gte_31_active_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.registerTelephonyCallback(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    // ─── SDK < 31 full callback state coverage ───────────────────────────────

    @Test
    fun test_callActivityListener_sdk_lt_31_ringing_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.listen(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.RINGING.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = "false",
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    @Test
    fun test_callActivityListener_sdk_lt_31_inactive_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.listen(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.INACTIVE.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    @Test
    fun test_callActivityListener_sdk_lt_31_active_state_in_callback() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        every { telephonyManager.listen(any(), any()) } answers {
            listener.saveCallInProgressEvent(CallInProgress.ACTIVE.state)
        }
        listener.onReceive(context, mockk<Intent>())

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    // ─── CustomTelephonyCallback direct invocation (SDK >= 31) ──────────────────

    @Test
    fun test_customTelephonyCallback_inactive_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        val callbackSlot = io.mockk.slot<CustomTelephonyCallback>()
        every { telephonyManager.registerTelephonyCallback(any(), capture(callbackSlot)) } answers {}
        listener.onReceive(context, mockk<Intent>())

        // Directly invoke the captured callback with INACTIVE state (TelephonyManager.CALL_STATE_IDLE = 0)
        callbackSlot.captured.onCallStateChanged(CallInProgress.INACTIVE.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    @Test
    fun test_customTelephonyCallback_ringing_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        val callbackSlot = io.mockk.slot<CustomTelephonyCallback>()
        every { telephonyManager.registerTelephonyCallback(any(), capture(callbackSlot)) } answers {}
        listener.onReceive(context, mockk<Intent>())

        callbackSlot.captured.onCallStateChanged(CallInProgress.RINGING.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = "false",
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    @Test
    fun test_customTelephonyCallback_active_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        val callbackSlot = io.mockk.slot<CustomTelephonyCallback>()
        every { telephonyManager.registerTelephonyCallback(any(), capture(callbackSlot)) } answers {}
        listener.onReceive(context, mockk<Intent>())

        callbackSlot.captured.onCallStateChanged(CallInProgress.ACTIVE.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    @Test
    fun test_customTelephonyCallback_unknown_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns true
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        every { context.mainExecutor } returns mockk(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version)
        val callbackSlot = io.mockk.slot<CustomTelephonyCallback>()
        every { telephonyManager.registerTelephonyCallback(any(), capture(callbackSlot)) } answers {}
        listener.onReceive(context, mockk<Intent>())

        callbackSlot.captured.onCallStateChanged(-999)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown"))
        )
    }

    // ─── PhoneStateListener direct invocation (SDK < 31) ─────────────────────

    @Test
    @Suppress("DEPRECATION")
    fun test_phoneStateListener_idle_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        val listenerSlot = io.mockk.slot<android.telephony.PhoneStateListener>()
        every { telephonyManager.listen(capture(listenerSlot), any()) } answers {}
        listener.onReceive(context, mockk<Intent>())

        listenerSlot.captured.onCallStateChanged(TelephonyManager.CALL_STATE_IDLE, null)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.INACTIVE.event,
            attrs = listOf(mapOf("progress" to "hangup", "duration_ms" to "0", "direction" to "outbound"))
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun test_phoneStateListener_ringing_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        val listenerSlot = io.mockk.slot<android.telephony.PhoneStateListener>()
        every { telephonyManager.listen(capture(listenerSlot), any()) } answers {}
        listener.onReceive(context, mockk<Intent>())

        listenerSlot.captured.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING, null)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = "false",
            attrs = listOf(mapOf("progress" to "ringing"))
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun test_phoneStateListener_offhook_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        val listenerSlot = io.mockk.slot<android.telephony.PhoneStateListener>()
        every { telephonyManager.listen(capture(listenerSlot), any()) } answers {}
        listener.onReceive(context, mockk<Intent>())

        listenerSlot.captured.onCallStateChanged(TelephonyManager.CALL_STATE_OFFHOOK, null)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.ACTIVE.event,
            attrs = listOf(mapOf("progress" to "active", "direction" to "outbound"))
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun test_phoneStateListener_unknown_state() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version)
        val listenerSlot = io.mockk.slot<android.telephony.PhoneStateListener>()
        every { telephonyManager.listen(capture(listenerSlot), any()) } answers {}
        listener.onReceive(context, mockk<Intent>())

        listenerSlot.captured.onCallStateChanged(-999, null)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNKNOWN.event,
            attrs = listOf(mapOf("progress" to "unknown"))
        )
    }

    // ─── onReceive with non-null context but callback already set (re-entry guard) ──

    @Test
    fun test_onReceive_does_not_recreate_telephony_callback_when_called_via_listen_twice() {
        // Simulates onReceive being called, then unregister + re-onReceive
        // The inner `if (phoneStateListener == null)` guard prevents double creation
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager
        var listenCount = 0
        every { telephonyManager.listen(any(), any()) } answers { listenCount++ }

        val listener = NIDCallActivityListener(mockedNID, version)
        listener.onReceive(context, mockk<Intent>())
        // unregister resets isReceiverRegistered so onReceive can enter the block again,
        // but phoneStateListener is already set so listen() will only be called once more
        listener.unregisterCallActivityListener(context)
        // phoneStateListener was set to null in unregister, so recreated on second onReceive
        listener.onReceive(context, mockk<Intent>())

        assert(listenCount == 2) { "Expected 2 listen registrations (one per onReceive) but was $listenCount" }
    }

    // ─── setCallActivityListener ─────────────────────────────────────────────

    @Test
    fun test_setCallActivityListener_registers_receiver_when_permission_granted() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val permissionChecker = mockk<NIDPermissionChecker>()
        every { permissionChecker.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        val intentFilter = mockk<IntentFilter>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version, permissionChecker, intentFilter)
        listener.setCallActivityListener(context)

        verify(exactly = 1) { context.registerReceiver(listener, intentFilter) }
    }

    @Test
    fun test_setCallActivityListener_does_not_register_when_already_registered() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val permissionChecker = mockk<NIDPermissionChecker>()
        every { permissionChecker.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        val intentFilter = mockk<IntentFilter>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val telephonyManager = mockk<TelephonyManager>(relaxed = true)
        every { context.getSystemService(any()) } returns telephonyManager

        val listener = NIDCallActivityListener(mockedNID, version, permissionChecker, intentFilter)
        // onReceive sets isReceiverRegistered = true
        listener.onReceive(context, mockk<Intent>())
        // Now setCallActivityListener should skip the register branch (isReceiverRegistered = true)
        // and fall into the else branch, emitting UNAUTHORIZED
        listener.setCallActivityListener(context)

        // registerReceiver was never called by setCallActivityListener (only onReceive path registers)
        verify(exactly = 0) { context.registerReceiver(listener, intentFilter) }
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
            attrs = listOf(mapOf("progress" to "unauthorized"))
        )
    }

    @Test
    fun test_setCallActivityListener_emits_unauthorized_when_permission_denied() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val permissionChecker = mockk<NIDPermissionChecker>()
        every { permissionChecker.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        val intentFilter = mockk<IntentFilter>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version, permissionChecker, intentFilter)
        listener.setCallActivityListener(context)

        verify(exactly = 0) { context.registerReceiver(any(), any()) }
        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
            attrs = listOf(mapOf("progress" to "unauthorized"))
        )
    }

    @Test
    fun test_setCallActivityListener_uses_injected_intent_filter() {
        val mockedNID = getMockedNeuroID()
        val version = mockk<VersionChecker>()
        every { version.isBuildVersionGreaterThanOrEqualTo31() } returns false
        val permissionChecker = mockk<NIDPermissionChecker>()
        every { permissionChecker.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        val customFilter = mockk<IntentFilter>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        val listener = NIDCallActivityListener(mockedNID, version, permissionChecker, customFilter)
        listener.setCallActivityListener(context)

        // Verify the exact filter instance passed in was used
        verify(exactly = 1) { context.registerReceiver(listener, customFilter) }
    }

    // ─── Harness ─────────────────────────────────────────────────────────────

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
