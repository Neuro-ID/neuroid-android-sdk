package com.neuroid.tracker.models

data class SessionStartResult(
    var started: Boolean,
    var identityId: String,
) {
    @Deprecated(
        "sessionID is deprecated",
        ReplaceWith("identityId"),
    )
    var sessionID: String = identityId
}
