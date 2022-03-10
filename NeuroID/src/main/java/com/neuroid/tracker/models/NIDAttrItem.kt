package com.neuroid.tracker.models

data class NIDAttrItem(
    val n: String,
    val v: String
) {
    fun getJson(): String {
        return "{\"n\":\"$n\",\"v\":\"$v\"}"
    }
}
