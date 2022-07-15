package com.neuroid.tracker.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.utils.NIDTimerActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import kotlin.random.Random

interface NIDDataStoreManager {
    fun saveEvent(event: NIDEventModel)
    fun getAllEvents(): List<String>
    fun addViewIdExclude(id: String)
    fun getNIDPreferences(): Flow<NIDPreferences>
    suspend fun clearEvents()
    suspend fun createSession(): NIDPreferences
    fun getSessionId(): String
    suspend fun setUserId(userId: String)
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NeuroID.getInstance().nidDataStoreManager
}

class NIDDataStoreManagerImpl(private val context: Context) : NIDDataStoreManager {
    companion object {
        private const val NID_STRING_EVENTS = "NID_STRING_EVENTS"
        private val NID_STRING_EVENTS_V2 = stringSetPreferencesKey("NID_STRING_EVENTS")
        private val NID_SESSION_ID = stringPreferencesKey("NID_SESSION_ID")
        private val NID_CLIENT_ID = stringPreferencesKey("NID_CLIENT_ID")
        private val NID_USER_ID = stringPreferencesKey("NID_USER_ID")
        private val NID_DEVICE_ID = stringPreferencesKey("NID_DEVICE_ID")
        private val NID_INTERMEDIATE_ID = stringPreferencesKey("NID_INTERMEDIATE_ID")
    }


    private var sharedPref: SharedPreferences? = null
    private val sharedLock = Semaphore(1)
    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    private val listIdsExcluded = arrayListOf<String>()
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val nidTimerActive = NIDTimerActive(this)

    private var nidPreferences = NIDPreferences("", "", "", "", "", emptySet())

    init {
        nidTimerActive.initTimer()
    }

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
            val strEvent = event.getOwnJson()

            if (NIDJobServiceManager.userActive.not()) {
                NIDJobServiceManager.userActive = true
                NIDJobServiceManager.restart()
            }

            if (!listNonActiveEvents.any { strEvent.contains(it) }) {
                nidTimerActive.restartTimerActive()
            }

            CoroutineScope(Dispatchers.IO).launch {
                context.dataStore.edit { preferences ->
                    val events: Set<String> = preferences[NID_STRING_EVENTS_V2] ?: emptySet()
                    val newEvents = LinkedHashSet<String>()
                    newEvents.addAll(events)
                    newEvents.add(strEvent)
                    preferences[NID_STRING_EVENTS_V2] = newEvents
                }
            }
        }
    }

    override fun getAllEvents(): List<String> {
        sharedLock.acquire()
        val lastEvents = sharedPref?.getString(NID_STRING_EVENTS, "").orEmpty()

        sharedPref?.let {
            with(it.edit()) {
                putString(NID_STRING_EVENTS, "")
                apply()
            }
        }

        sharedLock.release()

        return if (lastEvents.isEmpty()) {
            listOf()
        } else {
            lastEvents.split("|")
        }
    }

    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    override fun getNIDPreferences(): Flow<NIDPreferences> {
        return context.dataStore.data.catch {
            emit(emptyPreferences())
        }.map { preference ->
            val sessionId = preference[NID_SESSION_ID] ?: ""
            val clientId = preference[NID_CLIENT_ID] ?: ""
            val userId = preference[NID_USER_ID] ?: ""
            val deviceId = preference[NID_DEVICE_ID] ?: ""
            val intermediateId = preference[NID_INTERMEDIATE_ID] ?: ""
            val events = preference[NID_STRING_EVENTS_V2] ?: emptySet()
            NIDPreferences(
                sessionId = sessionId,
                clientId = clientId,
                userId = userId,
                deviceId = deviceId,
                intermediateId = intermediateId,
                events = events
            )
        }
    }

    override suspend fun clearEvents() {
        context.dataStore.edit { preferences ->
            preferences[NID_STRING_EVENTS_V2] = emptySet()
        }
    }

    override suspend fun createSession(): NIDPreferences {
        context.dataStore.edit { preference ->
            var sessionId = getNewSessionID()
            val clientId = preference[NID_CLIENT_ID] ?: getID()
            val userId = preference[NID_USER_ID] ?: "null"
            val deviceId = preference[NID_DEVICE_ID] ?: getID()
            val intermediateId = preference[NID_INTERMEDIATE_ID] ?: getID()

            preference[NID_SESSION_ID] = sessionId
            preference[NID_CLIENT_ID] = clientId
            preference[NID_USER_ID] = userId
            preference[NID_DEVICE_ID] = deviceId
            preference[NID_INTERMEDIATE_ID] = intermediateId
            nidPreferences = nidPreferences.copy(
                sessionId = sessionId,
                clientId = clientId,
                userId = userId,
                deviceId = deviceId,
                intermediateId = intermediateId
            )
        }
        return nidPreferences
    }

    private fun getID(): String {
        val timeNow = System.currentTimeMillis()
        val numRnd = Random.nextDouble() * Int.MAX_VALUE

        return "$timeNow.$numRnd"
    }

    private fun getNewSessionID(): String {
        var sid = ""
        repeat((1..16).count()) {
            sid += "${(0..9).random()}"
        }
        return sid
    }

    override fun getSessionId(): String {
        return nidPreferences.sessionId
    }

    override suspend fun setUserId(userId: String) {
        context.dataStore.edit { preference ->
            preference[NID_USER_ID] = userId
        }
    }

}
