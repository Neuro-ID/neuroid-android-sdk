package com.neuroid.tracker.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

interface NIDDataStoreManager {
    fun saveEvent(event: String)
    fun getAllEvents(): List<String>
}

fun getDataStoreInstance(context: Context): NIDDataStoreManager {
    NIDDataStoreManagerImp.init(context)
    return NIDDataStoreManagerImp
}

private object NIDDataStoreManagerImp: NIDDataStoreManager {
    private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
    private const val NID_STRING_EVENTS = "NID_STRING_EVENTS"
    private lateinit var sharedPref: SharedPreferences

    fun init(context: Context) {
        if(this::sharedPref.isInitialized.not()) {
            sharedPref =if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    }

    private fun getKeyAlias() = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    override fun saveEvent(event: String) {
        val lastEvents = sharedPref.getString(NID_STRING_EVENTS, "").orEmpty()
        val newStringEvents = if (lastEvents.isEmpty()) {
            event
        } else {
            "$lastEvents,$event"
        }

        with (sharedPref.edit()) {
            putString(NID_STRING_EVENTS, newStringEvents)
            commit()
        }
    }

    override fun getAllEvents(): List<String> {
        val lastEvents = sharedPref.getString(NID_STRING_EVENTS, "").orEmpty()
        with (sharedPref.edit()) {
            putString(NID_STRING_EVENTS, "")
            commit()
        }
        return if (lastEvents.isEmpty()) {
            listOf()
        } else {
            lastEvents.split(",")
        }
    }
}