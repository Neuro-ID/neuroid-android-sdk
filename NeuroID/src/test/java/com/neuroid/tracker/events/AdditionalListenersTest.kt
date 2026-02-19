package com.neuroid.tracker.events

import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.getMockedNeuroID
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AdditionalListenersTest {
    private lateinit var additionalListeners: AdditionalListeners
    private lateinit var logger: NIDLogWrapper
    private lateinit var neuroID: NeuroID

    @Before
    fun setup() {
        logger = mockk<NIDLogWrapper>(relaxed = true)
        every { logger.d(any(), any()) } just runs
        every { logger.e(any(), any()) } just runs

        neuroID = getMockedNeuroID()

        additionalListeners = AdditionalListeners(logger)
    }

    // addSelectOnSelect Tests
    @Test
    fun test_addSelectOnSelect_onItemSelected() {
        val idName = "test-select"
        val simpleClassName = "Spinner"
        val position = 2
        val lastSelectListener = mockk<AdapterView.OnItemSelectedListener>(relaxed = true)
        val adapter = mockk<AdapterView<*>>()
        val view = mockk<View>()

        // Create listener
        val listener = additionalListeners.addSelectOnSelect(
            neuroID,
            idName,
            lastSelectListener,
            simpleClassName
        )

        // Trigger onItemSelected
        listener.onItemSelected(adapter, view, position, 0L)

        // Verify last listener was called
        verify(exactly = 1) {
            lastSelectListener.onItemSelected(adapter, view, position, 0L)
        }

        // Verify captureEvent was called with correct parameters
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = SELECT_CHANGE,
                ts = any(),
                attrs = any(),
                tg = match {
                    it["etn"] == simpleClassName &&
                    it["tgs"] == idName &&
                    it["sender"] == simpleClassName
                },
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = "$position",
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
    }

    @Test
    fun test_addSelectOnSelect_onNothingSelected() {
        val idName = "test-select"
        val simpleClassName = "Spinner"
        val lastSelectListener = mockk<AdapterView.OnItemSelectedListener>(relaxed = true)
        val adapter = mockk<AdapterView<*>>()

        // Create listener
        val listener = additionalListeners.addSelectOnSelect(
            neuroID,
            idName,
            lastSelectListener,
            simpleClassName
        )

        // Trigger onNothingSelected
        listener.onNothingSelected(adapter)

        // Verify last listener was called
        verify(exactly = 1) {
            lastSelectListener.onNothingSelected(adapter)
        }

        // Verify captureEvent was NOT called
        verify(exactly = 0) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = any(),
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = any(),
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
    }

    @Test
    fun test_addSelectOnSelect_withNullLastListener() {
        val idName = "test-select"
        val simpleClassName = "Spinner"
        val position = 1
        val adapter = mockk<AdapterView<*>>()
        val view = mockk<View>()

        // Create listener with null lastSelectListener
        val listener = additionalListeners.addSelectOnSelect(
            neuroID,
            idName,
            null,
            simpleClassName
        )

        // Trigger onItemSelected (should not crash)
        listener.onItemSelected(adapter, view, position, 0L)

        // Verify captureEvent was still called
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = SELECT_CHANGE,
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = "$position",
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
    }

    // addSelectOnClickListener Tests
    @Test
    fun test_addSelectOnClickListener_onItemClick() {
        val idName = "test-click"
        val position = 3
        val lastClickListener = mockk<AdapterView.OnItemClickListener>(relaxed = true)
        val adapter = mockk<AdapterView<*>>()
        val view = mockk<View>()

        // Create listener
        val listener = additionalListeners.addSelectOnClickListener(
            neuroID,
            idName,
            lastClickListener
        )

        // Trigger onItemClick
        listener.onItemClick(adapter, view, position, 0L)

        // Verify last listener was called
        verify(exactly = 1) {
            lastClickListener.onItemClick(adapter, view, position, 0L)
        }

        // Verify captureEvent was called with correct parameters
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = SELECT_CHANGE,
                ts = any(),
                attrs = any(),
                tg = match {
                    it["etn"] == "INPUT" &&
                    it["et"] == "text"
                },
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = "$position",
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
    }

    @Test
    fun test_addSelectOnClickListener_withNullLastListener() {
        val idName = "test-click"
        val position = 0
        val adapter = mockk<AdapterView<*>>()
        val view = mockk<View>()

        // Create listener with null lastClickListener
        val listener = additionalListeners.addSelectOnClickListener(
            neuroID,
            idName,
            null
        )

        // Trigger onItemClick (should not crash)
        listener.onItemClick(adapter, view, position, 0L)

        // Verify captureEvent was still called
        verify(exactly = 1) {
            neuroID.captureEvent(
                queuedEvent = any(),
                type = SELECT_CHANGE,
                ts = any(),
                attrs = any(),
                tg = any(),
                tgs = idName,
                touches = any(),
                key = any(),
                gyro = any(),
                accel = any(),
                v = "$position",
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
    }

    // addExtraActionMenuListener Tests
    @Test
    fun test_addExtraActionMenuListener_setsCallback() {
        val view = mockk<EditText>(relaxed = true)
        val existingCallback = mockk<android.view.ActionMode.Callback>()

        every { view.customInsertionActionModeCallback } returns existingCallback

        // Call method
        additionalListeners.addExtraActionMenuListener(neuroID, view)

        // Verify customInsertionActionModeCallback was set
        verify(exactly = 1) {
            view.customInsertionActionModeCallback = any()
        }
    }

    @Test
    fun test_addExtraActionMenuListener_doesNotReplaceNIDCallback() {
        val view = mockk<EditText>(relaxed = true)
        val existingNIDCallback = mockk<com.neuroid.tracker.callbacks.NIDLongPressContextMenuCallbacks>()

        every { view.customInsertionActionModeCallback } returns existingNIDCallback

        // Call method
        additionalListeners.addExtraActionMenuListener(neuroID, view)

        // Verify customInsertionActionModeCallback was NOT set (already NID callback)
        verify(exactly = 0) {
            view.customInsertionActionModeCallback = any()
        }
    }
}

