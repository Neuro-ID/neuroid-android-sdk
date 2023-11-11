package com.neuroid.tracker.storage

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.util.Locale
import kotlin.random.Random

class NIDSharedPrefsDefaults(
    context: Context
) {
    private var sharedPref =
        context.getSharedPreferences(NID_SHARED_PREF_FILE, Context.MODE_PRIVATE)

    fun getSessionID(): String {
        return getString(NID_SID)
    }

    fun getNewSessionID(): String {
        val sid = UUID.randomUUID().toString()
        putString(NID_SID, sid)

        return sid
    }

    fun getClientId(): String {
        var cid = getString(NID_CID)
        return if (cid == "") {
            cid = UUID.randomUUID().toString()
            putString(NID_CID, cid)
            cid
        } else {
            cid
        }
    }

    fun resetClientId(): String {
        return this.getClientId()
    }

    fun setUserId(userId: String) {
        putString(NID_UID, userId)
    }

    // Must be set to null string
    fun getUserId(): Any? {
        val uid = getString(NID_UID)

        return uid.ifBlank { JSONObject.NULL }
    }

    /**
     * Shared device salt used for all strings
     */
    fun getDeviceSalt(): String {
        return getString(NID_DEVICE_SALT)
    }

    fun putDeviceSalt(salt: String): String {
        putString(NID_DEVICE_SALT, salt)
        return salt
    }

    fun getDeviceId(): String {
        var deviceId = getString(NID_DID)

        return if (deviceId == "") {
            deviceId = getID()
            putString(NID_DID, deviceId)

            deviceId
        } else {
            deviceId
        }
    }

    fun getIntermediateId(): String {
        var iid = getString(NID_IID, "")

        return if (iid == "") {
            iid = getID()
            putString(NID_IID, iid)
            iid
        } else {
            iid
        }
    }

    fun generateUniqueHexId(): String {
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

    private fun getString(key: String, default: String = ""): String {
        return sharedPref?.getString(key, "") ?: default
    }

    companion object {
        private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
        private const val NID_UID = "NID_UID_KEY"
        private const val NID_SID = "NID_SID_KEY"
        private const val NID_CID_OLD = "NID_CID_KEY"
        private const val NID_CID = "NID_CID_GUID_KEY"
        private const val NID_DID = "NID_DID_KEY"
        private const val NID_IID = "NID_IID_KEY"
        private const val NID_DEVICE_SALT = "NID_DEVICE_SALT"

        fun getHexRandomID(): String = List(12) {
            (('a'..'f') + ('0'..'9')).random()
        }.joinToString("")

        fun getDisplayWidth() =
            Resources.getSystem().displayMetrics.widthPixels

        fun getDisplayHeight() =
            Resources.getSystem().displayMetrics.heightPixels

    }
}