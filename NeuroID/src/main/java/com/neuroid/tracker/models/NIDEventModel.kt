package com.neuroid.tracker.models

import org.json.JSONObject

data class NIDEventModel(
    val type: String,
    val tg: String? = null,
    val tgs: HashMap<String,String>? = null,
    val key: String? = null,
    val v: String? = null,
    val en: String? = null,
    val etn: String? = null,
    val et: String? = null,
    var eid: String? = null,
    val ts: Long,
    val x: Float? = null,
    val y: Float? = null,
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
    val p: String? = null,
    val dnt: Boolean? = null,
    val tch: Boolean? = null,
    val url: String? = null,
    val ns: String? = null,
    val jsl: List<String>? = null,
    val jsv: String? = null,
    val uid: String? = null
) {
    fun getOwnJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("type", this.type)
        this.apply {
            tg?.let { jsonObject.put("tg", it) }
            tgs?.let {
                val childJson = JSONObject()
                it.forEach { (key, value) ->
                    childJson.put(key, value)
                }
                jsonObject.put("tgs", childJson)
            }
            key?.let { jsonObject.put("key", it) }
            v?.let { jsonObject.put("v", it) }
            en?.let { jsonObject.put("en", it) }
            etn?.let { jsonObject.put("etn", it) }
            et?.let { jsonObject.put("et", it) }
            eid?.let { jsonObject.put("eid", it) }
            jsonObject.put("ts", ts)
            x?.let { jsonObject.put("x", it) }
            y?.let { jsonObject.put("y", it) }
            f?.let { jsonObject.put("f", it) }
            lsid?.let { jsonObject.put("lsid", it) }
            sid?.let { jsonObject.put("sid", it) }
            siteId?.let { jsonObject.put("siteId", it) }
            cid?.let { jsonObject.put("cid", it) }
            did?.let { jsonObject.put("did", it) }
            iid?.let { jsonObject.put("iid", it) }
            loc?.let { jsonObject.put("loc", it) }
            ua?.let { jsonObject.put("ua", it) }
            tzo?.let { jsonObject.put("tzo", it) }
            lng?.let { jsonObject.put("lng", it) }
            p?.let { jsonObject.put("p", it) }
            dnt?.let { jsonObject.put("dnt", it) }
            tch?.let { jsonObject.put("tch", it) }
            url?.let { jsonObject.put("url", it) }
            ns?.let { jsonObject.put("ns", it) }
            jsl?.let { jsonObject.put("jsl", it) }
            jsv?.let { jsonObject.put("jsv", it) }
            uid?.let { jsonObject.put("uid", it) }
        }

        return jsonObject.toString()
    }
}
