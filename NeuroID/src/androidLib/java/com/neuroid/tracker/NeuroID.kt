package com.neuroid.tracker

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.*
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeuroID private constructor(
    private var application: Application,
    private var clientKey: String
) {
    private var firstTime = true
    private var endpoint = "https://api.neuro-id.com/v3/c"
    var nidDataStoreManager: NIDDataStoreManager = NIDDataStoreManagerImpl(application)

    @Synchronized
    private fun setupCallbacks() {
        if (firstTime) {
            firstTime = false
            application.let {
                it.registerActivityLifecycleCallbacks(NIDActivityCallbacks(nidDataStoreManager))
            }
        }
    }

    data class Builder(
        var application: Application,
        var clientKey: String = ""
    ) {
        fun build() =
            NeuroID(application, clientKey)
    }

    companion object {
        private lateinit var singleton: NeuroID

        @JvmStatic
        fun setNeuroIdInstance(neuroId: NeuroID) {
            singleton = neuroId
            singleton.setupCallbacks()
        }

        @JvmStatic
        fun getInstance() = singleton
    }

    fun setUserID(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            nidDataStoreManager.setUserId(userId)
        }
        nidDataStoreManager.saveEvent(
            NIDEventModel(
                type = SET_USER_ID,
                uid = userId,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun setScreenName(screen: String) {
        NIDServiceTracker.screenName = screen
    }

    fun excludeViewByResourceID(id: String) {
        nidDataStoreManager.addViewIdExclude(id)
    }

    fun getSessionId(): String {
        return nidDataStoreManager.getSessionId()
    }

    fun captureEvent(
        activity: AppCompatActivity,
        eventName: String,
        tgs: String
    ) {
        nidDataStoreManager.saveEvent(
            NIDEventModel(
                type = eventName,
                tgs = tgs,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun formSubmit() {
        nidDataStoreManager.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun formSubmitSuccess() {
        nidDataStoreManager.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_SUCCESS,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun formSubmitFailure() {
        nidDataStoreManager.saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_FAILURE,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun configureWithOptions(clientKey: String, endpoint: String) {
        this.endpoint = endpoint
        this.clientKey = clientKey
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            nidDataStoreManager.clearEvents() // Clean Events ?
        }
        createSession()
        NIDJobServiceManager.startJob(application, clientKey, endpoint)

    }

    fun stop() {
        NIDJobServiceManager.stopJob()
    }


    private fun createSession() {
        CoroutineScope(Dispatchers.IO).launch {
            val nidPreferences: NIDPreferences = nidDataStoreManager.createSession()
            nidDataStoreManager.saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = nidPreferences.sessionId,
                    lsid = "null",
                    cid = nidPreferences.clientId,
                    did = nidPreferences.deviceId,
                    iid = nidPreferences.intermediateId,
                    loc = nidPreferences.locale,
                    ua = nidPreferences.userAgent,
                    tzo = nidPreferences.timeZone,
                    lng = nidPreferences.language,
                    ce = true,
                    je = true,
                    ol = true,
                    p = nidPreferences.plataform,
                    jsl = listOf(),
                    dnt = false,
                    url = "",
                    ns = "nid",
                    jsv = NIDVersion.getSDKVersion(),
                    ts = System.currentTimeMillis(),
                )
            )
        }
    }
}
