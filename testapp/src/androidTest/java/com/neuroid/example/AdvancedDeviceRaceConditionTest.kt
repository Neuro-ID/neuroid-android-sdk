package com.neuroid.example

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedDeviceRaceConditionTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
    )

    private var mockWebServer: MockWebServer? = null
    private var eventRecorder: EventRecorder? = null
    private var attemptedRecorder: EventRecorder? = null
    @Before
    fun setup() {
        println("SETUP")
        mockWebServer = MockServerHolder.server
        eventRecorder = MockServerHolder.recorder
        attemptedRecorder = MockServerHolder.attemptedRecorder

     // clear any events queued from a previous test or SDK init
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        // pre-seed a valid unexpired ADV cache entry so getCachedID() fires ADVANCED_DEVICE_REQUEST
        // without requiring a real FPJS network call
         val prefs = ApplicationProvider.getApplicationContext<Context>()
             .getSharedPreferences("NID_SHARED_PREF_FILE", Context.MODE_PRIVATE)
         prefs.edit()
             .remove("NID_RID_KEY")
             //.putString("NID_RID_KEY", """{"key":"test-adv-id","exp":${Long.MAX_VALUE},"scr":"test-screen-name-x"}""".trimIndent())
             .apply()
    }

    @After
    fun teardown() {
        NeuroID.getInstance()?.stopSession()
        //mockWebServer?.shutdown()
    }

    /**
     * Detects the race where ADVANCED_DEVICE_REQUEST flushes a payload before
     * CREATE_SESSION is written, leaving the backend with an ADV-only session.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun advSignalDoesNotFlushWithoutCreateSession() = runTest(UnconfinedTestDispatcher(), timeout = 120.seconds) {

        val job = launch {
            repeat(4) { iteration ->
                eventRecorder?.clear()
                attemptedRecorder?.clear()

                val scenario = ActivityScenario.launch(Splash::class.java)

                NeuroID.getInstance()?.startSession("test-adv-race-${System.currentTimeMillis()}") { }

                // allow time for ADV network call, session events, and flush to complete
                Thread.sleep(10000)

                assertNotNull("eventRecorder was null — setup failed", eventRecorder)
                assertNotNull("attemptedRecorder was null — setup failed", attemptedRecorder)
                println("Iteration $iteration event counts: ${eventRecorder!!.eventTypeCounts()}")
                println("Iteration $iteration attempted event counts: ${attemptedRecorder!!.eventTypeCounts()}")

                // if no ADV events arrived the orphan check is vacuously passing — the ADV path is not being exercised
                assertTrue(
                    "Iteration $iteration: No ADVANCED_DEVICE_REQUEST events received — " +
                        "ADV path not exercised. Received types: ${eventRecorder!!.allReceivedEventTypes()}",
                    eventRecorder!!.hasReceivedEventType("ADVANCED_DEVICE_REQUEST")
                )
                assertTrue(
                    "Iteration $iteration: SDK did not attempt ADVANCED_DEVICE_REQUEST send — " +
                        "attempted types: ${attemptedRecorder!!.allReceivedEventTypes()}",
                    attemptedRecorder!!.hasReceivedEventType("ADVANCED_DEVICE_REQUEST")
                )

                val sendVsReceiveComparison = attemptedRecorder!!.compareTo(eventRecorder!!)
                assertTrue(
                    "Iteration $iteration: Attempted vs received mismatch. " +
                        "payloads(expected=${sendVsReceiveComparison.payloadCountExpected}, actual=${sendVsReceiveComparison.payloadCountActual}), " +
                        "missingTypes=${sendVsReceiveComparison.missingEventTypes}, " +
                        "extraTypes=${sendVsReceiveComparison.extraEventTypes}, " +
                        "missingSessionType=${sendVsReceiveComparison.missingBySessionAndType}, " +
                        "extraSessionType=${sendVsReceiveComparison.extraBySessionAndType}, " +
                        "attemptedCounts=${sendVsReceiveComparison.expectedTypeCounts}, " +
                        "receivedCounts=${sendVsReceiveComparison.actualTypeCounts}",
                    !sendVsReceiveComparison.isMatch()
                )

                val orphaned = eventRecorder!!.verifyNoOrphanedAdvSignals()
                println("ORPHANED $orphaned")
                assertTrue(
                    "Iteration $iteration: Found ${orphaned.size} payload(s) with ADVANCED_DEVICE_REQUEST but no CREATE_SESSION — " +
                        "ADV flushed before session was established. Affected siteIds: $orphaned",
                    orphaned.isEmpty()
                )
                println("Iteration $iteration event counts: ${eventRecorder!!.eventTypeCounts()}")
                //eventRecorder!!.verifyEventList(0, 0)
                scenario.close()
                Thread.sleep(10000)
                // 1. Kill the app process entirely
//                InstrumentationRegistry.getInstrumentation().uiAutomation
//                    .executeShellCommand("am force-stop com.neuroid.example.debug")
//                    .also { java.io.FileInputStream(it.fileDescriptor).use { s -> s.readBytes() }; it.close() }
//
//                Thread.sleep(1000)
//                val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
//                    .executeShellCommand(
//                        "am broadcast -a com.neuroid.example.SIMULATE_PUSH " +
//                                "-n com.neuroid.example.debug/com.neuroid.example.SimulatedPushReceiver " +
//                                "--es message \"test-push\""
//                    )
//                val output = java.io.FileInputStream(pfd.fileDescriptor).use {
//                    it.readBytes().toString(Charsets.UTF_8)
//                }
//                pfd.close()
//                println("broadcast result: $output")
//                Thread.sleep(1000)  // give onReceive time to execute
            }
        }
        job.join()
    }
}

