package com.neuroid.tracker.service

internal class StateStore {
    private var identityId: String? = null

    fun getIdentityId(): String? = identityId

    fun setIdentityId(value: String?) {
        identityId = value
    }
}
