package com.neuroid.tracker.extensions

import com.neuroid.tracker.utils.NIDSingletonIDs
import java.security.MessageDigest

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

fun String.getSHA256withSalt(): String {
    val saltedValue = NIDSingletonIDs.getSalt() + this
    return if (saltedValue.isBlank()) {
        ""
    } else {
        val bytes = saltedValue.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
