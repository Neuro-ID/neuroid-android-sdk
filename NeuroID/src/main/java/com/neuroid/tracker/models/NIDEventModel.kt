package com.neuroid.tracker.models

import org.json.JSONArray
import org.json.JSONObject

data class NIDEventModel(
    val type: String,
    val attrs: JSONArray? = null,
    val tg: Map<String, Any>? = null,
    val tgs: String? = null,
    val touches: List<String>? = null,
    val key: String? = null,
    val gyro: NIDSensorModel? = null,
    val accel: NIDSensorModel? = null,
    val v: String? = null,
    val hv: String? = null,
    val en: String? = null,
    val etn: String? = null,
    val ec: String? = null,
    val et: String? = null,
    var eid: String? = null,
    val ct: String? = null,
    val ts: Long,
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
    var url: String? = null,
    val ns: String? = null,
    val jsl: List<String>? = null,
    val jsv: String? = null,
    val uid: String? = null,
    val o: String? = null,
    var rts: String? = null,
    val metadata: JSONObject? = null
) : Comparable<NIDEventModel> {
    fun getOwnJson(): String {
        return getJSONObject().toString()
    }
    fun getJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", this.type)
        this.apply {
            tg?.let {
                jsonObject.put("tg", JSONObject(it))
            }
            attrs?.let { jsonObject.put("attrs", it) }
            tgs?.let { jsonObject.put("tgs", it) }
            touches?.let {
                val array = JSONArray()
                it.forEach { item ->
                    array.put(JSONObject(item))
                }
                jsonObject.put("touches", array)
            }
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
            rts?.let {jsonObject.put("rts", it)}
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
            gyro?.let {
                jsonObject.put("gyro", it.getJsonObject())
            }
            accel?.let {
                jsonObject.put("accel", it.getJsonObject())
            }
            metadata?.let {
                jsonObject.put("metadata", it)
            }
        }

        return jsonObject
    }

    override fun compareTo(other: NIDEventModel): Int {
        return ts.compareTo(other.ts)
    }
}

data class NIDSensorModel(
    var x: Float?,
    var y: Float?,
    var z: Float?
) {
    fun getJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("x", x ?: JSONObject.NULL)
        jsonObject.put("y", y ?: JSONObject.NULL)
        jsonObject.put("z", z ?: JSONObject.NULL)

        return jsonObject
    }
}
