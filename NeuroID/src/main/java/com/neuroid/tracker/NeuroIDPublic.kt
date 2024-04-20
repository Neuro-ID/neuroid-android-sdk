package com.neuroid.tracker

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.neuroid.tracker.models.SessionStartResult

interface NeuroIDPublic {
    /**
     * Deprecated, do not use!
     */
    @Deprecated("Replaced with getUserID", ReplaceWith("getUserID()"))
    fun getClientId():String

    /**
     * Return the currently set client id (use this one).
     */
    fun getClientID():String

    /**
     * Set a tag for the widget that is not registered.
     */
    fun setTestURL(newEndpoint: String)

    /**
     * Set a dev testing URL for testing.
     */
    @VisibleForTesting
    fun setTestingNeuroIDDevURL()

    /**
     * Set a user id.
     */
    fun setUserID(userID: String): Boolean

    /**
     * Return the currently set user id.
     */
    fun getUserId(): String
    /**
     * Return the currently set user id.
     */
    fun getUserID(): String
    /**
     * Return the currently set registered user id.
     */
    fun getRegisteredUserID(): String

    /**
     * Set a registered user id.
     */
    fun setRegisteredUserID(registeredUserId: String): Boolean

    /**
     * Set a screen name for the current page in a session.
     */
    fun setScreenName(screen: String): Boolean

    /**
     * Return the currently set screen name for the current page in a session.
     */
    fun getScreenName(): String

    /**
     * Register page targets. This will send events targets to the server for the current page.
     */
    fun registerPageTargets(activity: Activity)

    /**
     * Exclude the event that is specified. Used for testing purposes.
     */
    @VisibleForTesting
    fun excludeViewByTestID(id: String)

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setEnvironment is deprecated and no longer required")
    fun setEnvironment(environment: String)

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setEnvironmentProduction is deprecated and no longer required")
    fun setEnvironmentProduction(prod: Boolean)

    /**
     * Return the current environment set for operation (live/test).
     */
    fun getEnvironment(): String

    /**
     * Deprecated, do not use!
     */
    @Deprecated("setSiteId is deprecated and no longer required")
    fun setSiteId(siteId: String)

    /**
     * Return the current sessionId.
     */
    fun getSessionId(): String

    /**
     * Deprecated, do not use!
     */
    @Deprecated("formSubmit is deprecated and no longer required")
    fun formSubmit()

    /**
     * Deprecated, do not use!
     */
    @Deprecated("formSubmitSuccess is deprecated and no longer required")
    fun formSubmitSuccess()

    /**
     * Deprecated, do not use
     */
    @Deprecated("formSubmitFailure is deprecated and no longer required")
    fun formSubmitFailure()

    /**
     * Start the SDK and start a new session and use the userID as the sessionID. Return true if
     * that indicates that the SDK is started. Returns false if the SDK is stopped. In the case of
     * a false return, check that the client key is set properly in the NeuroID.Builder() call.
     */
    fun start(): Boolean

    /**
     * Stop the SDK and close the current session. Return true to indicate that
     * the SDK is stopped.
     */
    fun stop(): Boolean

    /**
     * Stop the SDK and closes the current session.
     */
    fun closeSession()

    /**
     * Return true if the SDK is stopped and false if SDK is not stopped.
     */
    fun isStopped(): Boolean

    /**
     * Set the ReactNative flag to true.
     */
    fun setIsRN()

    /**
     * Enable/disable logging for the SDK, This is set true on startup. This turns on/off
     * all logging levels (error, debug, verbose, info and warn) that are generated in logcat.
     */
    fun enableLogging(enable: Boolean)

    /**
     * Return the NeuroID SDK version.
     */
    fun getSDKVersion(): String

    /**
     * Clear the currently set user and register user id.
     */
    fun clearSessionVariables()

    /**
     * Stop the SDK and close the current session. Return true to indicate that the SDK is stopped.
     */
    fun stopSession(): Boolean

    /**
     * Restart the current session and continue to collect events that was paused by
     * pauseCollection(). This will not start a new session.
     */
    fun resumeCollection()

    /**
     * Pause the currently running session. This will stop event collection in the current session.
     * The current session will not be closed. The session can be restarted by calling
     * restartSession()
     */
    fun pauseCollection()

    /**
     * Start a new session. This will start the SDK and start a new session using the session ID
     * that is passed in. If a session ID is not passed in, a session ID at random will be used.
     * Return a session result that contains the session ID and a boolean
     * indicating the started state of the SDK.
     */
    fun startSession(sessionID: String? = null): SessionStartResult
}
