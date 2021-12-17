package com.neuroid.tracker.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.utils.NIDTimerActive
import java.util.concurrent.Semaphore

interface NIDDataStoreManager {
    fun saveEvent(event: String)
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
        sharedPref = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            EncryptedSharedPreferences.create(
                NID_SHARED_PREF_FILE,
                getKeyAlias(),
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            // TODO (Diego Maldonado): Create method for versions less than api 23
            context.getSharedPreferences(NID_SHARED_PREF_FILE, Context.MODE_PRIVATE)
        }
    }

    private fun getKeyAlias() = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    @Synchronized
    override fun saveEvent(event: String) {
        sharedLock.acquire()
        if (!listNonActiveEvents.any { event.contains(it) }) {
            NIDTimerActive.restartTimerActive()
        }

        val lastEvents = sharedPref?.getString(NID_STRING_EVENTS, "").orEmpty()
        val newStringEvents = if (lastEvents.isEmpty()) {
            event
        } else {
            "$lastEvents,$event"
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