package com.neuroid.tracker.extensions

import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.models.SessionStartResult

/**
 * Start the SDK and start a new session using the userID as the sessionID. Takes in a boolean to
 * enable/disable the advanced signal collection. Return true to indicate that the SDK is started.
 * Return false if not started.
 */

@Deprecated(
    "Use start(completion) instead",
    ReplaceWith("start(completion)"),
)
fun NeuroIDPublic.start(
    advancedDeviceSignals: Boolean,
    completion: (Boolean) -> Unit = {},
) {
    start {
        completion(it)
    }
}

/**
 * Start a new session. This will start the SDK and start a new session using the session ID
 * that is passed in. If a session ID is not passed in, a session ID at random will be used.
 * Return a session result that contains the session ID and a boolean
 * indicating the started state of the SDK. Takes in a boolean to
 * enable/disable the advanced signal collection.
 */
@Deprecated(
    "Use startSession(sessionID, completion) instead",
    ReplaceWith("startSession(sessionID, completion)"),
)
fun NeuroIDPublic.startSession(
    sessionID: String? = null,
    advancedDeviceSignals: Boolean,
    completion: (SessionStartResult) -> Unit = {},
) {
    startSession(
        sessionID,
    ) {
        completion(it)
    }
}
