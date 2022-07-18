package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import com.neuroid.tracker.callbacks.NIDActivityCallbacks
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.storage.initDataStoreCtx
import com.neuroid.tracker.utils.NIDTimerActive
import com.neuroid.tracker.utils.NIDVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeuroID private constructor(
    private var application: Application?,
    private var clientKey: String
) {
    private var firstTime = true
    private var endpoint = "https://api.neuro-id.com/v3/c"
    private var sessionId = ""

    @Synchronized
    private fun setupCallbacks() {
        if (firstTime) {
            firstTime = false
            application?.let {
                initDataStoreCtx(it.applicationContext)
            }
        }
    }

    data class Builder(
        var application: Application? = null,
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
        application?.let {
            NIDSharedPrefsDefaults(it).setUserId(userId)
        }
        getDataStoreInstance().saveEvent(
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
        application?.let {
            getDataStoreInstance().addViewIdExclude(id)
        }
    }

    fun getSessionId(): String {
        return sessionId
    }

    fun captureEvent(eventName: String, tgs: String) {
        application?.applicationContext?.let {
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = eventName,
                    tgs = tgs,
                    ts = System.currentTimeMillis()
                )
            )
        }
    }

    fun formSubmit() {
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun formSubmitSuccess() {
        getDataStoreInstance().saveEvent(
            NIDEventModel(
                type = FORM_SUBMIT_SUCCESS,
                ts = System.currentTimeMillis()
            )
        )
    }

    fun formSubmitFailure() {
        getDataStoreInstance().saveEvent(
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
            getDataStoreInstance().clearEvents() // Clean Events ?
            createSession()
        }
        application?.let {
            NIDJobServiceManager.startJob(it, clientKey, endpoint)
        }
    }

    fun stop() {
        NIDJobServiceManager.stopJob()
    }

    fun registerAllViewsForCallerActivity(activity: Activity) {
        NIDTimerActive.initTimer()
        activity.application.registerActivityLifecycleCallbacks(
            NIDActivityCallbacks(
                activity::class.java.name,
                activity.resources.configuration.orientation
            )
        )
        registerLaterLifecycleFragments(activity)
    }

    private suspend fun createSession() {
        application?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)
            sessionId = sharedDefaults.getNewSessionID()
            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = sessionId,
                    lsid = "null",
                    cid = sharedDefaults.getClientId(),
                    did = sharedDefaults.getDeviceId(),
                    iid = sharedDefaults.getIntermediateId(),
                    loc = sharedDefaults.getLocale(),
                    ua = sharedDefaults.getUserAgent(),
                    tzo = sharedDefaults.getTimeZone(),
                    lng = sharedDefaults.getLanguage(),
                    ce = true,
                    je = true,
                    ol = true,
                    p = sharedDefaults.getPlatform(),
                    jsl = listOf(),
                    dnt = false,
                    url = "",
                    ns = "nid",
                    jsv = NIDVersion.getSDKVersion(),
                    ts = System.currentTimeMillis()
                )
            )
        }
    }
}
