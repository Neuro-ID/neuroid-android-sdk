package com.neuroid.tracker.utils

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import java.util.UUID

object NIDSingletonIDs {
    private var saltId = ""

    fun getSalt(): String = saltId

    /**
     * Look at shared defaults, and
     */
    fun retrieveOrCreateLocalSalt(): String {
        var context = NeuroID.getInternalInstance()?.getApplicationContext()
        val sharedDefaults = context?.let { NIDSharedPrefsDefaults(it) }
        var existingSalt = sharedDefaults?.getDeviceSalt()

        if (existingSalt != null) {
            if (existingSalt.isNotBlank()) {
                saltId = existingSalt
                return existingSalt
            }
        }
        saltId = UUID.randomUUID().toString()
        sharedDefaults?.putDeviceSalt(saltId)
        return saltId
    }
}
