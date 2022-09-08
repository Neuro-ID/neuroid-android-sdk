package com.neuroid.tracker.utils

import com.neuroid.tracker.extensions.getSHA256
import org.json.JSONArray
import org.json.JSONObject

class JsonUtils {
    companion object {
        fun getAttrJson(text: String?): JSONArray {
            val value = JSONObject().put("n", "v").put("v", "S~C~~${text?.length ?: 0}")
            val hash =
                JSONObject().put("n", "hash").put("v", text?.getSHA256()?.take(8))
            return JSONArray().put(value).put(hash)
        }
    }
}