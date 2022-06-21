package com.neuroid.tracker.extensions

import android.util.Base64
import java.security.MessageDigest

fun String.encodeToBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

fun String.getSHA256(): String {
    return if (this.isBlank()) {
        ""
    } else {
        val bytes = this.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
