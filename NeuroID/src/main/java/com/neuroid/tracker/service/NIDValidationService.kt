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

    fun scrubIdentifier(identifier: String): String {
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val ssnRegex = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")

        // Check for email address
        val emailMatch = emailRegex.find(identifier)
        if (emailMatch != null) {
            val email = emailMatch.value
            val atIndex = email.indexOf('@')
            val maskedEmail = email[0] + "*".repeat(atIndex - 2) + email[atIndex - 1] + email.substring(atIndex)
            return identifier.replace(email, maskedEmail)
        }

        // Check for SSN
        val ssnMatch = ssnRegex.find(identifier)
        if (ssnMatch != null) {
            return "***-**-****"
        }

        // If no email or SSN is found, return the original string
        return identifier
    }
}
