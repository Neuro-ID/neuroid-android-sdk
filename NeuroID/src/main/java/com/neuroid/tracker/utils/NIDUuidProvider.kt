package com.neuroid.tracker.utils

import java.util.UUID

interface NIDUuidProvider {
    fun randomUUID(): String
    fun nameUUIDFromBytes(name: ByteArray): String
}

class NIDSystemUuidProvider : NIDUuidProvider {
    override fun randomUUID(): String = UUID.randomUUID().toString()
    override fun nameUUIDFromBytes(name: ByteArray): String = UUID.nameUUIDFromBytes(name).toString()
}