package com.neuroid.tracker.utils

import android.app.Activity
import java.util.UUID

fun Activity.getGUID(): String {
    val hashCodeAct = this.hashCode()
    return UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()
}