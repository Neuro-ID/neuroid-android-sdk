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

class NeuroID private constructor(
    private var application: Application?,
    private var clientKey: String
) {
    private var firstTime = true

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
                uid = userId ?: "",
                ts = System.currentTimeMillis()
            )
        )
    }

    fun setScreenName(screen: String) {
        NIDServiceTracker.screenName = screen
    }

    fun getSessionId(): String {
        var sid = ""
        application?.let {
            sid = NIDSharedPrefsDefaults(it).getSessionID()
        }

        return sid
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

    fun start() {
        getDataStoreInstance().getAllEvents() // Clean Events ?
        createSession()
        application?.let {
            NIDJobServiceManager.startJob(it, clientKey)
        }
    }

    fun stop() {
        NIDJobServiceManager.stopJob()
    }

    fun registerAllViewsForCallerActivity(activity: Activity) {
        NIDTimerActive.initTimer()
        activity.application.registerActivityLifecycleCallbacks(NIDActivityCallbacks(
            activity::class.java.name,
            activity.resources.configuration.orientation
        ))
        registerLaterLifecycleFragments(activity)
    }

    private fun createSession() {
        application?.let {
            val sharedDefaults = NIDSharedPrefsDefaults(it)

            getDataStoreInstance().saveEvent(
                NIDEventModel(
                    type = CREATE_SESSION,
                    f = clientKey,
                    sid = sharedDefaults.getNewSessionID(),
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
                    jsv = "4.android-1.2.1",
                    ts = System.currentTimeMillis()
                )
            )
        }
    }
}