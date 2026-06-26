package com.neuroid.tracker.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
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
import io.mockk.verify
import org.junit.After
import org.junit.Test
import java.lang.reflect.Field
import java.util.concurrent.Executor

class NIDCallActivityListenerTests {

    // NIDCallActivityListener.registerCustomTelephonyCallback now branches on
    // Build.VERSION.SDK_INT directly. On the JVM that field defaults to 0, so the
    // API >= 31 (TelephonyCallback) path is unreachable unless we override it.
    // We use Unsafe (accessed reflectively, since it isn't on the compile classpath)
    // to set the (non-constant) static field and restore it afterwards.
    private val originalSdkInt = Build.VERSION.SDK_INT

    private fun setSdkInt(value: Int) {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafe = unsafeClass.getDeclaredField("theUnsafe").apply {
            isAccessible = true
        }.get(null)
        val field = Build.VERSION::class.java.getDeclaredField("SDK_INT")
        val base = unsafeClass
            .getMethod("staticFieldBase", Field::class.java)
            .invoke(theUnsafe, field)
        val offset = unsafeClass
            .getMethod("staticFieldOffset", Field::class.java)
            .invoke(theUnsafe, field) as Long
        unsafeClass
            .getMethod(
                "putInt",
                Any::class.java,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            .invoke(theUnsafe, base, offset, value)
    }

    @After
    fun tearDown() {
        setSdkInt(originalSdkInt)
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
        setSdkInt(Build.VERSION_CODES.S)
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
    // saveCallInProgressEvent()
    //
    // Only CONNECTED / DISCONNECTED / UNAUTHORIZED are tracked. Ringing and
    // unknown states are no-ops (no event captured) and no attrs are emitted.
    // ---------------------------------------------------------------------

    @Test
    fun test_saveCallInProgressEvent_connected_capturesConnected() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.saveCallInProgressEvent(CallInProgress.CONNECTED.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.CONNECTED.event,
        )
    }

    @Test
    fun test_saveCallInProgressEvent_disconnected_capturesDisconnected() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.saveCallInProgressEvent(CallInProgress.DISCONNECTED.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.DISCONNECTED.event,
        )
    }

    @Test
    fun test_saveCallInProgressEvent_unauthorized_capturesUnauthorized() {
        val mockedNID = getMockedNeuroID()
        val listener = NIDCallActivityListener(mockedNID, mockk())

        listener.saveCallInProgressEvent(CallInProgress.UNAUTHORIZED.state)

        verifyCaptureEvent(
            mockedNID,
            CALL_IN_PROGRESS,
            cp = CallInProgress.UNAUTHORIZED.event,
        )
    }

    // ---------------------------------------------------------------------
    // registerCustomTelephonyCallback() + CustomTelephonyCallback.onCallStateChanged()
    // (API >= 31) — invokes the REAL captured callback to exercise the state mapping
    // ---------------------------------------------------------------------

    @Test
    fun test_customTelephonyCallback_idle_capturesDisconnected() {
        sdk31RealCallbackHarness(
            TelephonyManager.CALL_STATE_IDLE,
            CallInProgress.DISCONNECTED.event,
            eventExpected = true,
        )
    }

    @Test
    fun test_customTelephonyCallback_offhook_capturesConnected() {
        sdk31RealCallbackHarness(
            TelephonyManager.CALL_STATE_OFFHOOK,
            CallInProgress.CONNECTED.event,
            eventExpected = true,
        )
    }

    @Test
    fun test_customTelephonyCallback_ringing_capturesNoEvent() {
        // ringing is no longer tracked
        sdk31RealCallbackHarness(
            TelephonyManager.CALL_STATE_RINGING,
            null,
            eventExpected = false,
        )
    }

    @Test
    fun test_customTelephonyCallback_unknownState_capturesNoEvent() {
        // unknown states are no longer tracked
        sdk31RealCallbackHarness(
            777,
            null,
            eventExpected = false,
        )
    }

    private fun sdk31RealCallbackHarness(
        callState: Int,
        expectedCp: String?,
        eventExpected: Boolean,
    ) {
        setSdkInt(Build.VERSION_CODES.S)
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
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, cp = expectedCp)
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
            eventExpected = true,
        )
    }

    @Test
    fun test_phoneStateListener_offhook_capturesConnected() {
        sdkLess31RealListenerHarness(
            TelephonyManager.CALL_STATE_OFFHOOK,
            CallInProgress.CONNECTED.event,
            eventExpected = true,
        )
    }

    @Test
    fun test_phoneStateListener_ringing_capturesNoEvent() {
        // ringing is no longer tracked
        sdkLess31RealListenerHarness(
            TelephonyManager.CALL_STATE_RINGING,
            null,
            eventExpected = false,
        )
    }

    @Test
    fun test_phoneStateListener_unknownState_capturesNoEvent() {
        // unknown states are no longer tracked
        sdkLess31RealListenerHarness(
            777,
            null,
            eventExpected = false,
        )
    }

    private fun sdkLess31RealListenerHarness(
        callState: Int,
        expectedCp: String?,
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
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, cp = expectedCp)
        } else {
            verifyCaptureEvent(mockedNID, CALL_IN_PROGRESS, count = 0)
        }
    }
}
