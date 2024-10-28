package com.neuroid.tracker.models

import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.events.ADVANCED_DEVICE_REQUEST
import com.neuroid.tracker.events.APPLICATION_METADATA
import com.neuroid.tracker.events.APPLICATION_SUBMIT
import com.neuroid.tracker.events.ATTEMPTED_LOGIN
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.events.CLOSE_SESSION
import com.neuroid.tracker.events.CONTEXT_MENU
import com.neuroid.tracker.events.COPY
import com.neuroid.tracker.events.CREATE_SESSION
import com.neuroid.tracker.events.CUT
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.INPUT
import com.neuroid.tracker.events.KEY_DOWN
import com.neuroid.tracker.events.KEY_UP
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.MOBILE_METADATA_ANDROID
import com.neuroid.tracker.events.NETWORK_STATE
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.events.PAUSE_EVENT_CAPTURE
import com.neuroid.tracker.events.REGISTER_TARGET
import com.neuroid.tracker.events.RESUME_EVENT_CAPTURE
import com.neuroid.tracker.events.SELECT_CHANGE
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.events.TEXT_CHANGE
import com.neuroid.tracker.events.TOUCH_END
import com.neuroid.tracker.events.TOUCH_MOVE
import com.neuroid.tracker.events.TOUCH_START
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.events.WINDOW_FOCUS
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.utils.Constants
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDMetaData
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class NIDEventModel(
    val type: String,
    val ts: Long =
        Calendar.getInstance().timeInMillis, // Default 0 because the DataStore.saveEvent method will
    // always add real timestamp
    val attrs: List<Map<String, Any>>? = null,
    val tg: Map<String, Any>? = null,
    val tgs: String? = null,
    val touches: List<NIDTouchModel>? = null,
    val key: String? = null,
    val gyro: NIDSensorModel? = NIDSensorHelper.getGyroscopeInfo(),
    val accel: NIDSensorModel? = NIDSensorHelper.getAccelerometerInfo(),
    val v: String? = null,
    val hv: String? = null,
    val en: String? = null,
    val etn: String? = null,
    val ec: String? = null,
    val et: String? = null,
    var eid: String? = null,
    val ct: String? = null,
    val sm: Int? = null,
    val pd: Int? = null,
    val x: Float? = null,
    val y: Float? = null,
    val w: Int? = null,
    val h: Int? = null,
    val sw: Float? = null,
    val sh: Float? = null,
    val f: String? = null,
    val lsid: String? = null,
    val sid: String? = null,
    val siteId: String? = null,
    val cid: String? = null,
    val did: String? = null,
    val iid: String? = null,
    val loc: String? = null,
    val ua: String? = null,
    val tzo: Int? = null,
    val lng: String? = null,
    val ce: Boolean? = null,
    val je: Boolean? = null,
    val ol: Boolean? = null,
    val p: String? = null,
    val dnt: Boolean? = null,
    val tch: Boolean? = null,
    val url: String? = null,
    val ns: String? = null,
    val jsl: List<String>? = null,
    val jsv: String? = null,
    val uid: String? = null,
    val o: String? = null,
    var rts: String? = null,
    val metadata: NIDMetaData? = null,
    val rid: String? = null,
    val m: String? = null,
    val level: String? = null,
    val c: Boolean? = null,
    val isWifi: Boolean? = null,
    val isConnected: Boolean? = null,
    val cp: String? = null,
    val l: Long? = null,
    val synthetic: Boolean? = null,
) : Comparable<NIDEventModel> {
    fun toJSONString(): String {
        return toJSON().toString()
    }

    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", this.type)
        this.apply {
            tg?.let { jsonObject.put("tg", JSONObject(it)) }
            attrs?.let { jsonObject.put("attrs", it) }
            tgs?.let { jsonObject.put("tgs", it) }
            touches?.let {
                val array = JSONArray()
                it.forEach { item -> array.put(item.toJSON()) }
                jsonObject.put("touches", array)
            }
            m?.let { jsonObject.put("m", it) }
            level?.let { jsonObject.put("level", it) }
            key?.let { jsonObject.put("key", it) }
            v?.let { jsonObject.put("v", it) }
            hv?.let { jsonObject.put("hv", it) }
            en?.let { jsonObject.put("en", it) }
            etn?.let { jsonObject.put("etn", it) }
            ec?.let { jsonObject.put("ec", it) }
            et?.let { jsonObject.put("et", it) }
            eid?.let { jsonObject.put("eid", it) }
            ct?.let { jsonObject.put("ct", it) }
            jsonObject.put("ts", ts)
            sm?.let { jsonObject.put("sm", it) }
            pd?.let { jsonObject.put("pd", it) }
            x?.let { jsonObject.put("x", it) }
            y?.let { jsonObject.put("y", it) }
            w?.let { jsonObject.put("w", it) }
            h?.let { jsonObject.put("h", it) }
            sw?.let { jsonObject.put("sw", it) }
            sh?.let { jsonObject.put("sh", it) }
            f?.let { jsonObject.put("f", it) }
            rts?.let { jsonObject.put("rts", it) }
            lsid?.let {
                if (it == "null") {
                    jsonObject.put("lsid", null)
                } else {
                    jsonObject.put("lsid", it)
                }
            }
            sid?.let { jsonObject.put("sid", it) }
            siteId?.let { jsonObject.put("siteId", it) }
            cid?.let { jsonObject.put("cid", it) }
            did?.let { jsonObject.put("did", it) }
            iid?.let { jsonObject.put("iid", it) }
            loc?.let { jsonObject.put("loc", it) }
            ua?.let { jsonObject.put("ua", it) }
            tzo?.let { jsonObject.put("tzo", it) }
            lng?.let { jsonObject.put("lng", it) }
            ce?.let { jsonObject.put("ce", it) }
            je?.let { jsonObject.put("je", it) }
            ol?.let { jsonObject.put("ol", it) }
            o?.let { jsonObject.put("o", it) }
            p?.let { jsonObject.put("p", it) }
            dnt?.let { jsonObject.put("dnt", it) }
            tch?.let { jsonObject.put("tch", it) }
            url?.let { jsonObject.put("url", it) }
            ns?.let { jsonObject.put("ns", it) }
            rid?.let { jsonObject.put("rid", it) }
            c?.let { jsonObject.put("c", it) }
            jsl?.let {
                val values = JSONArray()
                jsonObject.put("jsl", values)
            }
            jsv?.let {
                if (it == "null") {
                    jsonObject.put("jsv", null)
                } else {
                    jsonObject.put("jsv", it)
                }
            }
            uid?.let { jsonObject.put("uid", it) }
            gyro?.let { jsonObject.put("gyro", it.toJSON()) }
            accel?.let { jsonObject.put("accel", it.toJSON()) }
            metadata?.let { jsonObject.put("metadata", it) }
            isWifi?.let { jsonObject.put("iswifi", it) }
            isConnected?.let { jsonObject.put("isconnected", it) }
            cp?.let { jsonObject.put("cp", it) }
            l?.let { jsonObject.put("l", it) }
            synthetic?.let { jsonObject.put("synthetic", it) }
        }

        return jsonObject
    }

    override fun compareTo(other: NIDEventModel): Int {
        return ts.compareTo(other.ts)
    }

    internal fun log() {
        NIDLog.d(Constants.debugEventTag.displayName, "") {
            var contextString: String? = ""
            when (this.type) {
                PAUSE_EVENT_CAPTURE -> contextString = ""
                RESUME_EVENT_CAPTURE -> contextString = ""
                SET_USER_ID -> contextString = "uid=${this.uid}"
                CREATE_SESSION -> contextString = "cid=${this.cid}, sh=${this.sh}, sw=${this.sw}"
                APPLICATION_SUBMIT -> contextString = ""
                TEXT_CHANGE -> contextString = "v=${this.v}, tg=${this.tg}"
                "SET_CHECKPOINT" -> contextString = ""
                "STATE_CHANGE" -> contextString = this.url ?: ""
                KEY_UP -> contextString = "tg=${this.tg}"
                KEY_DOWN -> contextString = "tg=${this.tg}"
                INPUT -> contextString = "v=${this.v}, tg=${this.tg}"
                FOCUS -> contextString = ""
                BLUR -> contextString = ""
                MOBILE_METADATA_ANDROID -> contextString = "meta=${this.metadata}"
                "CLICK" -> contextString = ""
                REGISTER_TARGET ->
                    contextString =
                        "et=${this.et}, rts=${this.rts}, ec=${this.ec} v=${this.v} tg=${this.tg} meta=${this.metadata} attrs=[${this.attrs}]"
                "DEREGISTER_TARGET" -> contextString = ""
                TOUCH_START -> contextString = "xy=${this.touches} tg=${this.tg} tgs=${this.tgs} ec=${this.ec} syn=${this.synthetic}"
                TOUCH_END -> contextString = "xy=${this.touches} tg=${this.tg} tgs=${this.tgs} ec=${this.ec} syn=${this.synthetic}"
                TOUCH_MOVE -> contextString = "xy=${this.touches} tg=${this.tg} tgs=${this.tgs} ec=${this.ec} syn=${this.synthetic}"
                CLOSE_SESSION -> contextString = ""
                SET_VARIABLE -> contextString = this.v ?: ""
                CUT -> contextString = ""
                COPY -> contextString = ""
                PASTE -> contextString = ""
                WINDOW_RESIZE -> contextString = "h=${this.h}, w=${this.w}"
                SELECT_CHANGE -> contextString = "tg=${this.tg}"
                WINDOW_LOAD -> contextString = "meta=${this.metadata}, attrs=${this.attrs}, ec=${this.ec}"
                WINDOW_UNLOAD -> contextString = "meta=${this.metadata}, attrs=${this.attrs}, ec=${this.ec}"
                WINDOW_BLUR -> contextString = "meta=${this.metadata}"
                WINDOW_FOCUS -> contextString = "meta=${this.metadata}"
                CONTEXT_MENU -> contextString = "meta=${this.metadata}"
                ADVANCED_DEVICE_REQUEST -> contextString = "rid=${this.rid}, c=${this.c}, l=${this.l}, ct=${this.ct}"
                LOG -> contextString = "m=${this.m}, ts=${this.ts}, level=${this.level}"
                NETWORK_STATE -> contextString = "iswifi=${this.isWifi}, isconnected=${this.isConnected}"
                ATTEMPTED_LOGIN -> contextString = "uid=${this.uid}"
                CALL_IN_PROGRESS -> contextString = "cp=${this.cp}, metadata=${this.attrs}"
                APPLICATION_METADATA -> contextString = "attrs=${this.attrs}"
                else -> {}
            }

            "EVENT: ${this.type} - ${this.tgs} - $contextString"
        }
    }
}

data class NIDSensorModel(val x: Float?, val y: Float?, val z: Float?) {
    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("x", x ?: JSONObject.NULL)
        jsonObject.put("y", y ?: JSONObject.NULL)
        jsonObject.put("z", z ?: JSONObject.NULL)

        return jsonObject
    }
}

data class NIDTouchModel(val tid: Float?, val x: Float?, val y: Float?) {
    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("tid", tid ?: JSONObject.NULL)
        jsonObject.put("x", x ?: JSONObject.NULL)
        jsonObject.put("y", y ?: JSONObject.NULL)

        return jsonObject
    }
}
