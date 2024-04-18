package com.neuroid.tracker

import android.app.Activity
import com.neuroid.tracker.models.SessionStartResult

interface NeuroID {

    fun getClientId():String
    fun getClientID():String
    fun setTestURL(newEndpoint: String)
    fun setTestingNeuroIDDevURL()
    fun setUserID(userID: String): Boolean
    fun getUserId(): String
    fun getUserID(): String
    fun getRegisteredUserID(): String
    fun setRegisteredUserID(registeredUserId: String): Boolean
    fun setScreenName(screen: String): Boolean
    fun getScreenName(): String
    fun registerPageTargets(activity: Activity)
    fun formSubmit()
    fun excludeViewByTestID(id: String)
    fun setEnvironment(environment: String)
    fun setEnvironmentProduction(prod: Boolean)
    fun getEnvironment(): String
    fun setSiteId(siteId: String)
    fun getSessionId(): String
    fun formSubmitSuccess()
    fun formSubmitFailure()
    fun start(): Boolean
    fun stop(): Boolean
    fun closeSession()
    fun isStopped(): Boolean
    fun setIsRN()
    fun enableLogging(enable: Boolean)
    fun getSDKVersion(): String
    fun clearSessionVariables()
    fun stopSession(): Boolean
    fun resumeCollection()
    fun pauseCollection()
    fun startSession(sessionID: String? = null): SessionStartResult
}