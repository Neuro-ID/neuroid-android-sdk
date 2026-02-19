package com.neuroid.tracker.events

import android.view.View
import android.widget.AbsSpinner
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SingleTargetListenerRegisterTest {
    private lateinit var singleTargetRegister: SingleTargetListenerRegister
    private lateinit var neuroID: NeuroID
    private lateinit var logger: NIDLogWrapper
    private lateinit var additionalListeners: AdditionalListeners

    @Before
    fun setup() {
        logger = mockk<NIDLogWrapper>(relaxed = true)
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        neuroID = getMockedNeuroID()
        additionalListeners = AdditionalListeners(logger)

        singleTargetRegister = SingleTargetListenerRegister(neuroID, logger, additionalListeners)
    }

    // Initialization Tests
    @Test
    fun test_singleTargetListenerRegister_initialization() {
        assertEquals(neuroID, singleTargetRegister.neuroID)
        assertEquals(logger, singleTargetRegister.logger)
        assertEquals(additionalListeners, singleTargetRegister.additionalListeners)
    }

    // createAtrrList Tests
    @Test
    fun test_createAtrrList_structure() {
        val view = mockk<View>(relaxed = true)
        val guid = "test-guid-123"
        val idName = "test-id"
        val activityOrFragment = "TestActivity"
        val parent = "MainActivity"

        every { view.context } returns mockk(relaxed = true)

        // Call method
        val result = singleTargetRegister.createAtrrList(
            view,
            guid,
            idName,
            activityOrFragment,
            parent
        )

        // Verify list has 5 elements
        assertEquals(5, result.size)

        // Verify guid element
        val guidElement = result[0]
        assertEquals("guid", guidElement["n"])
        assertEquals(guid, guidElement["v"])

        // Verify screenHierarchy element
        val screenHierarchyElement = result[1]
        assertEquals("screenHierarchy", screenHierarchyElement["n"])

        // Verify top-screenHierarchy element
        val topScreenHierarchyElement = result[2]
        assertEquals("top-screenHierarchy", topScreenHierarchyElement["n"])
        assertEquals("/$parent/$idName", topScreenHierarchyElement["v"])

        // Verify parentClass element
        val parentClassElement = result[3]
        assertEquals("parentClass", parentClassElement["n"])
        assertEquals(parent, parentClassElement["v"])

        // Verify component element
        val componentElement = result[4]
        assertEquals("component", componentElement["n"])
        assertEquals(activityOrFragment, componentElement["v"])
    }

    @Test
    fun test_createAtrrList_withEmptyParameters() {
        val view = mockk<View>(relaxed = true)
        val guid = "guid-456"
        val idName = "button-1"

        every { view.context } returns mockk(relaxed = true)

        // Call method with empty optional parameters
        val result = singleTargetRegister.createAtrrList(
            view,
            guid,
            idName,
            activityOrFragment = "",
            parent = ""
        )

        // Verify list still has 5 elements
        assertEquals(5, result.size)

        // Verify empty values are preserved
        val parentClassElement = result[3]
        assertEquals("", parentClassElement["v"])

        val componentElement = result[4]
        assertEquals("", componentElement["v"])
    }

    // registerFinalComponent Tests
    @Test
    fun test_registerFinalComponent_capturesEvent() {
        val idName = "test-button"
        val et = "button"
        val v = "Submit"
        val simpleName = "Button"
        val attrJson = listOf(
            mapOf("n" to "guid", "v" to "test-guid")
        )

        // Set static values
        NeuroID.screenActivityName = "MainActivity"
        NeuroID.screenFragName = ""
        NeuroID.screenName = "main"

        var completionCalled = false
        val onComplete = { completionCalled = true }

        // Call method
        singleTargetRegister.registerFinalComponent(
            rts = null,
            idName = idName,
            et = et,
            v = v,
            simpleName = simpleName,
            attrJson = attrJson,
            onComplete = onComplete
        )

        // Verify captureEvent was called
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = REGISTER_TARGET,
                ts = any(),
                attrs = attrJson,
                tg = mapOf("attr" to attrJson),
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = v,
                hv = any(),
                en = idName,
                etn = "INPUT",
                ec = NeuroID.screenName,
                et = any(),
                eid = idName,
                ct = any(),
                sm = any(),
                pd = any(),
                x = any(),
                y = any(),
                w = any(),
                h = any(),
                sw = any(),
                sh = any(),
                f = any(),
                lsid = any(),
                sid = any(),
                siteId = any(),
                cid = any(),
                did = any(),
                iid = any(),
                loc = any(),
                ua = any(),
                tzo = any(),
                lng = any(),
                ce = any(),
                je = any(),
                ol = any(),
                p = any(),
                dnt = any(),
                tch = any(),
                url = any(),
                ns = any(),
                jsl = any(),
                jsv = any(),
                uid = any(),
                o = any(),
                rts = any(),
                metadata = any(),
                rid = any(),
                m = any(),
                level = any(),
                c = any(),
                isWifi = any(),
                isConnected = any(),
                cp = any(),
                l = any(),
                synthetic = any(),
            )
        }

        // Verify completion callback was called
        assertEquals(true, completionCalled)
    }

    @Test
    fun test_registerFinalComponent_withFragmentName() {
        val idName = "fragment-button"
        val et = "button"
        val v = "Next"
        val simpleName = "Button"
        val attrJson = listOf<Map<String, Any>>()

        // Set static values with fragment
        NeuroID.screenActivityName = "MainActivity"
        NeuroID.screenFragName = "ProfileFragment"
        NeuroID.screenName = "profile"

        // Call method
        singleTargetRegister.registerFinalComponent(
            rts = "test-rts",
            idName = idName,
            et = et,
            v = v,
            simpleName = simpleName,
            attrJson = attrJson
        )

        // Verify captureEvent was called with fragment in URL
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = REGISTER_TARGET,
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = any(),
                hv = any(),
                en = any(),
                etn = any(),
                ec = any(),
                et = any(),
                eid = any(),
                ct = any(),
                sm = any(),
                pd = any(),
                x = any(),
                y = any(),
                w = any(),
                h = any(),
                sw = any(),
                sh = any(),
                f = any(),
                lsid = any(),
                sid = any(),
                siteId = any(),
                cid = any(),
                did = any(),
                iid = any(),
                loc = any(),
                ua = any(),
                tzo = any(),
                lng = any(),
                ce = any(),
                je = any(),
                ol = any(),
                p = any(),
                dnt = any(),
                tch = any(),
                url = match { it.contains("/ProfileFragment/")},
                ns = any(),
                jsl = any(),
                jsv = any(),
                uid = any(),
                o = any(),
                rts = "test-rts",
                metadata = any(),
                rid = any(),
                m = any(),
                level = any(),
                c = any(),
                isWifi = any(),
                isConnected = any(),
                cp = any(),
                l = any(),
                synthetic = any(),
            )
        }
    }
}

