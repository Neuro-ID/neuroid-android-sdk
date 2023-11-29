package com.neuroid.tracker.utils

import android.app.Activity
import java.util.UUID

fun Activity.getGUID(): String {
    val hashCodeAct = this.hashCode()
    return UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()
}

internal fun generateUniqueHexId(): String {
    // use random UUID to ensure uniqueness amongst devices,
    return UUID.randomUUID().toString()
}