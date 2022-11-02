package com.neuroid.tracker.utils

import java.util.*

object NIDSingletonIDs {
    private var saltId = ""

    fun getSalt(): String = saltId
    fun updateSalt() {
        saltId = UUID.randomUUID().toString()
    }
}