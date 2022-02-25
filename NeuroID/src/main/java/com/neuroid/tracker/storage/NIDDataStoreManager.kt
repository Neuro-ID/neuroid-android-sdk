package com.neuroid.tracker.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.utils.NIDTimerActive
import java.util.concurrent.Semaphore

interface NIDDataStoreManager {
    fun saveEvent(event: NIDEventModel)
    fun getAllEvents(): List<String>
}

fun initDataStoreCtx(context: Context) {
    NIDDataStoreManagerImp.init(context)
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NIDDataStoreManagerImp
}

private object NIDDataStoreManagerImp: NIDDataStoreManager {
    private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
    private const val NID_STRING_EVENTS = "NID_STRING_EVENTS"
    private var sharedPref: SharedPreferences? = null
    private val sharedLock = Semaphore(1)
    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )

    fun init(context: Context) {
        sharedPref = EncryptedSharedPreferences.create(
            context,
            NID_SHARED_PREF_FILE,
            getKeyAlias(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getKeyAlias(context: Context) = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        val strEvent = event.getOwnJson()

        if (NIDJobServiceManager.userActive.not()) {
            NIDJobServiceManager.userActive = true
            NIDJobServiceManager.restart()
        }

        sharedLock.acquire()
        if (!listNonActiveEvents.any { strEvent.contains(it) }) {
            NIDTimerActive.restartTimerActive()
        }

        val lastEvents = sharedPref?.getString(NID_STRING_EVENTS, "").orEmpty()
        val newStringEvents = if (lastEvents.isEmpty()) {
            strEvent
        } else {
            "$lastEvents,$strEvent"
        }

        sharedPref?.let {
            with (it.edit()) {
                putString(NID_STRING_EVENTS, newStringEvents)
                apply()
            }
        }

        sharedLock.release()
    }

    override fun getAllEvents(): List<String> {
        sharedLock.acquire()
        val lastEvents = sharedPref?.getString(NID_STRING_EVENTS, "").orEmpty()

        sharedPref?.let {
            with (it.edit()) {
                putString(NID_STRING_EVENTS, "")
                apply()
            }
        }

        sharedLock.release()

        return if (lastEvents.isEmpty()) {
            listOf()
        } else {
            lastEvents.split(",")
        }
    }
}