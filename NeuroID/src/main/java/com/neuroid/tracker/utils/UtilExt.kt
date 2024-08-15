package com.neuroid.tracker.utils

import android.app.Activity
import java.util.UUID

fun Activity.getGUID(): String {
    val hashCodeAct = this.hashCode()
    return UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()
}

internal fun generateUniqueHexID(requireNIDPrefix: Boolean = false): String {
    // use random UUID to ensure uniqueness amongst devices,
    var nidPrefix = ""
    if (requireNIDPrefix) {
        nidPrefix = "nid-"
    }
    return "$nidPrefix${UUID.randomUUID()}"
}
