package com.neuroid.tracker.models

import com.google.common.base.Verify.verify
import com.neuroid.tracker.events.*
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.Constants.*
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NIDEventModelUnitTest {
    @Test
    fun testToJSONSimple() {
        val event = NIDEventModel(type = "TEST_TYPE", ts = 1)
        val result = event.toJSONString()

        assertEquals(
            "{\"type\":\"TEST_TYPE\",\"accel\":{\"x\":null,\"y\":null,\"z\":null},\"ts\":1,\"gyro\":{\"x\":null,\"y\":null,\"z\":null}}",
            result,
        )
    }

    @Test
    fun nidTouchModel() {
        val touchModel = NIDTouchModel(x = 15.5f, y = 25.5f, tid = 3f)
        val json = touchModel.toJSON()
        assertEquals(json.getDouble("x"), 15.5, 0.001)
        assertEquals(json.getDouble("y"), 25.5, 0.001)
        assertEquals(json.getDouble("tid"), 3.0, 0.001)
    }

    @Test
    fun testToJSONAdvanced() {
        val event =
            NIDEventModel(
                tgs = "tgs-a",
                touches = arrayListOf(NIDTouchModel(x = 10f, y = 20f, tid = 1f)),
                type = "TEST_TYPE",
                m = "m-a",
                level = "level-a",
                key = "key-a",
                v = "v-a",
                hv = "hv-a",
                en = "en-a",
                etn =  "etn-a",
                ec = "ec-a",
                et = "et-a",
                eid = "eid-a",
                ct = "ct-a",
                sm = 1,
                pd = 2,
                x = 1f,
                y = 2f,
                w = 3,
                h = 4,
                sw = 1f,
                sh = 2f,
                f = "f-a",
                rts = "rts-a",
                lsid = null,
                sid = "sid-a",
                siteId = "siteId-a",
                cid = "cid-a",
                did = "did-a",
                iid =  "iid-a",
                loc = "loc-a",
                ua = "ua-a",
                tzo = 420,
                lng = "lng-a",
                ce = true,
                je = true,
                ol = false,
                p = "p-a",
                tch = true,
                url = "url-a",
                ns = "ns-a",
                uid = "uid-a",
                o = "o-a",
                rid = "rid-a",
                c = true,
                isWifi = true,
                isConnected = true,
                cp = "cp-a",
                l = 12345L,
                cts = "cts-a",
                synthetic = false,
                gyro = NIDSensorModel(x = 1.1f, y = 2.2f, z = 3.3f),
                accel = NIDSensorModel(x = 4.4f, y = 5.5f, z = 6.6f),
                ts = 1,
                attrs =
                    listOf(
                        mapOf(
                            "key" to "value",
                        ),
                    ),
                tg = mapOf("key1" to "value1", "key2" to "value2"),
                dnt = false,
            )
        val result = event.toJSONString()
        val json = JSONObject(result)

        // Verify all the parameters that were set in the constructor
        assertEquals("TEST_TYPE", json.getString("type"))
        assertEquals("tgs-a", json.getString("tgs"))
        assertEquals("m-a", json.getString("m"))
        assertEquals("level-a", json.getString("level"))
        assertEquals("key-a", json.getString("key"))
        assertEquals("v-a", json.getString("v"))
        assertEquals("hv-a", json.getString("hv"))
        assertEquals("en-a", json.getString("en"))
        assertEquals("etn-a", json.getString("etn"))
        assertEquals("ec-a", json.getString("ec"))
        assertEquals("et-a", json.getString("et"))
        assertEquals("eid-a", json.getString("eid"))
        assertEquals("ct-a", json.getString("ct"))
        assertEquals(1, json.getInt("sm"))
        assertEquals(2, json.getInt("pd"))
        assertEquals(1.0, json.getDouble("x"), 0.001)
        assertEquals(2.0, json.getDouble("y"), 0.001)
        assertEquals(3, json.getInt("w"))
        assertEquals(4, json.getInt("h"))
        assertEquals(1.0, json.getDouble("sw"), 0.001)
        assertEquals(2.0, json.getDouble("sh"), 0.001)
        assertEquals("f-a", json.getString("f"))
        assertEquals("rts-a", json.getString("rts"))
        assertEquals("sid-a", json.getString("sid"))
        assertEquals("siteId-a", json.getString("siteId"))
        assertEquals("cid-a", json.getString("cid"))
        assertEquals("did-a", json.getString("did"))
        assertEquals("iid-a", json.getString("iid"))
        assertEquals("loc-a", json.getString("loc"))
        assertEquals("ua-a", json.getString("ua"))
        assertEquals(420, json.getInt("tzo"))
        assertEquals("lng-a", json.getString("lng"))
        assertTrue(json.getBoolean("ce"))
        assertTrue(json.getBoolean("je"))
        assertEquals(false, json.getBoolean("ol"))
        assertEquals("p-a", json.getString("p"))
        assertEquals(false, json.getBoolean("dnt"))
        assertTrue(json.getBoolean("tch"))
        assertEquals("url-a", json.getString("url"))
        assertEquals("ns-a", json.getString("ns"))
        assertEquals("uid-a", json.getString("uid"))
        assertEquals("o-a", json.getString("o"))
        assertEquals("rid-a", json.getString("rid"))
        assertTrue(json.getBoolean("c"))
        assertTrue(json.getBoolean("iswifi"))
        assertTrue(json.getBoolean("isconnected"))
        assertEquals("cp-a", json.getString("cp"))
        assertEquals(12345L, json.getLong("l"))
        assertEquals("cts-a", json.getString("cts"))
        assertEquals(false, json.getBoolean("synthetic"))
        assertEquals(1L, json.getLong("ts"))

        // Verify nested objects
        val tg = json.getJSONObject("tg")
        assertEquals("value1", tg.getString("key1"))
        assertEquals("value2", tg.getString("key2"))

        val attrs = json.getJSONArray("attrs")
        assertEquals(1, attrs.length())
        val attr = attrs.getJSONObject(0)
        assertEquals("value", attr.getString("key"))

        val touches = json.getJSONArray("touches")
        assertEquals(1, touches.length())
        val touch = touches.getJSONObject(0)
        assertEquals(1.0, touch.getDouble("tid"), 0.001)
        assertEquals(10.0, touch.getDouble("x"), 0.001)
        assertEquals(20.0, touch.getDouble("y"), 0.001)

        val gyro = json.getJSONObject("gyro")
        assertEquals(1.1, gyro.getDouble("x"), 0.001)
        assertEquals(2.2, gyro.getDouble("y"), 0.001)
        assertEquals(3.3, gyro.getDouble("z"), 0.001)

        val accel = json.getJSONObject("accel")
        assertEquals(4.4, accel.getDouble("x"), 0.001)
        assertEquals(5.5, accel.getDouble("y"), 0.001)
        assertEquals(6.6, accel.getDouble("z"), 0.001)

        // Verify lsid is not present (was set to null)
        assertEquals(false, json.has("lsid"))
    }
}
