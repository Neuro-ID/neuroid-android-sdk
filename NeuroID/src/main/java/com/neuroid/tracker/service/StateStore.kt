package com.neuroid.tracker.service

class StateStore {

    private var identityId: String? = null
    private var userID: String = ""

    fun getIdentityId(): String? = identityId

    fun setIdentityId(value: String?) {
        identityId = value
    }

    fun getUserID(): String = userID

    fun setUserID(value: String) {
        userID = value
    }
}