package com.neuroid.tracker.storage

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class NIDSharedPrefsDefaults(
    context: Application
) {
    private var sharedPref =
        context.getSharedPreferences(NID_SHARED_PREF_FILE, Context.MODE_PRIVATE)
    private var sequenceId = 1

    fun createRequestId(): String {
        val epoch = 1488084578518
        val now = System.currentTimeMillis()
        val rawId = (now - epoch) * 1024 + sequenceId
        sequenceId += 1

        return String.format("%02x", rawId)
    }

    suspend fun getSessionID(): String {
        return getString(NID_SID)
    }

    fun getNewSessionID(): String {
        var sid = ""

        repeat((1..16).count()) {
            sid += "${(0..9).random()}"
        }
        putString(NID_SID, sid)

        return sid
    }

    suspend fun getClientId(): String {
        var cid = getString(NID_CID)
        return if (cid == "") {
            cid = getID()
            putString(NID_CID, cid)
            cid
        } else {
            cid
        }
    }

    fun setUserId(userId: String) {
        putString(NID_UID, userId)
    }

    // Must be set to null string
    suspend fun getUserId(): String {
        val uid = getString(NID_UID, "null")

        return uid.ifBlank { "null" }
    }

    suspend fun getDeviceId(): String {
        var deviceId = getString(NID_DID)

        return if (deviceId == "") {
            deviceId = getID()
            putString(NID_DID, deviceId)

            deviceId
        } else {
            deviceId
        }
    }

    suspend fun getIntermediateId(): String {
        var iid = getString(NID_IID, "")

        return if (iid == "") {
            iid = getID()
            putString(NID_IID, iid)
            iid
        } else {
            iid
        }
    }

    fun getPageId(): String {
        val x = 1
        val now = System.currentTimeMillis()
        val rawId = (now - 1488084578518) * 1024 + (x + 1)

        return String.format("%02x", rawId)
    }

    fun getLocale(): String = Locale.getDefault().toString()

    fun getLanguage(): String = Locale.getDefault().language

    fun getUserAgent() = System.getProperty("http.agent").orEmpty()

    fun getTimeZone() = 300

    fun getPlatform() = "Android"

    private fun getID(): String {
        val timeNow = System.currentTimeMillis()
        val numRnd = Random.nextDouble() * Int.MAX_VALUE

        return "$timeNow.$numRnd"
    }

    private fun putString(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            sharedPref?.let {
                with(it.edit()) {
                    putString(key, value)
                    apply()
                }
            }
        }
    }

    private suspend fun getString(key: String, default: String = ""): String {
        return sharedPref?.getString(key, "") ?: default
    }

    companion object {
        private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
        private const val NID_UID = "NID_UID_KEY"
        private const val NID_SID = "NID_SID_KEY"
        private const val NID_CID = "NID_CID_KEY"
        private const val NID_DID = "NID_DID_KEY"
        private const val NID_IID = "NID_IID_KEY"
        private const val NID_TIME_TO_EXPIRE = "NID_TIME_TO_EXPIRE_KEY"
    }
}