package com.neuroid.tracker.extensions

import android.util.Base64

fun String.encodeToBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.DEFAULT)