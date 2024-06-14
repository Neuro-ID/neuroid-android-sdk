package com.neuroid.tracker.service

import com.neuroid.tracker.utils.NIDLogWrapper

internal class NIDValidationService(
    val logger: NIDLogWrapper,
) {
    fun validateClientKey(clientKey: String): Boolean {
        var valid = false
        val regex = "key_(live|test)_[A-Za-z0-9]+"

        if (clientKey.matches(regex.toRegex())) {
            valid = true
        }

        return valid
    }

    fun verifyClientKeyExists(clientKey: String?): Boolean {
        if (clientKey.isNullOrEmpty()) {
            logger.e(msg = "Missing Client Key - please call Builder.build() prior to calling a starting function")
            return false
        }
        return true
    }

    fun validateSiteID(siteID: String): Boolean {
        var valid = false
        val regex = "form_[a-zA-Z0-9]{5}\\d{3}\$"

        if (siteID.matches(regex.toRegex())) {
            valid = true
        }

        return valid
    }

    fun validateUserID(userID: String): Boolean {
        val regex = "^[a-zA-Z0-9-_.]{3,100}$"

        if (!userID.matches(regex.toRegex())) {
            logger.e(msg = "Invalid UserID")
            return false
        }

        return true
    }
}
