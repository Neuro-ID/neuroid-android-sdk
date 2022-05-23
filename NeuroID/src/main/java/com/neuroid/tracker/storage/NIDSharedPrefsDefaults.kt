package com.neuroid.tracker.storage

import android.app.Application
import android.content.Context
import java.util.*
import kotlin.random.Random

class NIDSharedPrefsDefaults(
    context: Application
) {
    private var sharedPref = context.getSharedPreferences(NID_SHARED_PREF_FILE, Context.MODE_PRIVATE)
    private var sequenceId = 1

    fun createRequestId(): String {
        val epoch = 1488084578518
        val now = System.currentTimeMillis()
        val rawId = (now - epoch) * 1024 + sequenceId
        sequenceId += 1

        return String.format("%02x", rawId)
    }

    fun getSessionID(): String {
        val savedSid = sharedPref?.getString(NID_SID, "").orEmpty()

        if (savedSid.isNotEmpty() && !isSessionExpired()) {
            return savedSid
        } else {
            var sid = ""

            repeat((1..16).count()) {
                sid += "${(0..9).random()}"
            }

            sharedPref?.let {
                with(it.edit()) {
                    putString(NID_SID, sid)
                    apply()
                }
            }

            return sid
        }
    }

    fun getNewSessionID(): String {
        var sid = ""

        repeat((1..16).count()) {
            sid += "${(0..9).random()}"
        }

        sharedPref?.let {
            with(it.edit()) {
                putString(NID_SID, sid)
                apply()
            }
        }

        return sid
    }

    private fun isSessionExpired(): Boolean {
        var timeToExpired = sharedPref?.getLong(NID_TIME_TO_EXPIRE, 0L) ?: 0L
        val actualTime = System.currentTimeMillis()

        if (timeToExpired == 0L) {
            timeToExpired = setTimeToExpire()
        }

        if(actualTime > timeToExpired) {
            setTimeToExpire()
        }

        return actualTime > timeToExpired
    }

    private fun setTimeToExpire(): Long {
        val timeToExpired = System.currentTimeMillis() + 1800000
        sharedPref?.let {
            with(it.edit()) {
                putLong(NID_TIME_TO_EXPIRE, timeToExpired)
                apply()
            }
        }

        return timeToExpired
    }

    fun getClientId(): String {
        var cid = sharedPref?.getString(NID_CID, "").orEmpty()
        return if (cid == "") {
            cid = getID()
            sharedPref?.let {
                with(it.edit()) {
                    putString(NID_CID, cid)
                    apply()
                }
            }

            cid
        } else {
            cid
        }
    }

    fun setUserId(userId: String) {
        sharedPref?.let {
            with(it.edit()) {
                putString(NID_UID, userId)
                apply()
            }
        }
    }

    fun getUserId() = sharedPref?.getString(NID_UID, "") ?: ""

    fun getDeviceId(): String {
        var deviceId = sharedPref?.getString(NID_DID, "").orEmpty()

        return if (deviceId == "") {
            deviceId = getID()
            sharedPref?.let {
                with(it.edit()) {
                    putString(NID_DID, deviceId)
                    apply()
                }
            }

            deviceId
        } else {
            deviceId
        }
    }

    fun getIntermediateId(): String {
        var iid = sharedPref?.getString(NID_IID, "").orEmpty()

        return if (iid == "") {
            iid = getID()
            sharedPref?.let {
                with(it.edit()) {
                    putString(NID_IID, iid)
                    apply()
                }
            }

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