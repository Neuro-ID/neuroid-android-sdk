package com.neuroid.tracker.storage

import android.content.Context
import androidx.core.content.edit
import com.neuroid.tracker.utils.NIDResourcesUtils
import com.neuroid.tracker.utils.NIDSystemUuidProvider
import com.neuroid.tracker.utils.NIDTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NIDSharedPrefsDefaults(
    context: Context,
    val nidTime: NIDTime = NIDTime(),
    val uuidProvider: NIDSystemUuidProvider = NIDSystemUuidProvider(),
    val resourcesProvider: NIDResourcesUtils = NIDResourcesUtils(),
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var sharedPref = context.getSharedPreferences(NID_SHARED_PREF_FILE, Context.MODE_PRIVATE)

    fun getClientID(): String {
        var cid = getString(NID_CID)
        return if (cid == "") {
            cid = uuidProvider.randomUUID()
            putString(NID_CID, cid)
            cid
        } else {
            cid
        }
    }

    fun resetClientID(): String = this.getClientID()

    /**
     * Shared device salt used for all strings
     */
    fun getDeviceSalt(): String = getString(NID_DEVICE_SALT)

    fun putDeviceSalt(salt: String): String {
        putString(NID_DEVICE_SALT, salt)
        return salt
    }

    fun getLocale() = resourcesProvider.getDefaultLocale()

    fun getLanguage() = resourcesProvider.getDefaultLanguage()

    fun getUserAgent() = resourcesProvider.getHttpAgent()

    fun getTimeZone() = 300

    fun getPlatform() = "Android"

    internal fun putString(
        key: String,
        value: String,
    ) {
        CoroutineScope(dispatcher).launch {
            sharedPref?.let {
                it.edit { putString(key, value) }
            }
        }
    }

    internal fun getString(
        key: String,
        default: String = "",
    ): String = sharedPref?.getString(key, "") ?: default

    internal fun getDisplayWidth() = resourcesProvider.getDisplayMetricsWidth()

    internal fun getDisplayHeight() = resourcesProvider.getDisplayMetricsHeight()

    companion object {
        private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
        private const val NID_CID = "NID_CID_GUID_KEY"
        private const val NID_DEVICE_SALT = "NID_DEVICE_SALT"
    }
}
