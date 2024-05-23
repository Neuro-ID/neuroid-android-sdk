package com.neuroid.tracker.utils

import com.neuroid.tracker.extensions.getSHA256withSalt

class JsonUtils {
    companion object {
        fun getAttrJson(text: String?): List<Map<String, String>> {
            val value =
                mapOf(
                    "n" to "v",
                    "v" to "S~C~~${text?.length ?: 0}",
                )

            val hash =
                mapOf(
                    "n" to "hash",
                    "v" to (text?.getSHA256withSalt()?.take(8) ?: ""),
                )

            return listOf(
                value,
                hash,
            )
        }
    }
}
