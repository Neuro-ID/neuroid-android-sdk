package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.events.NID_ORIGIN_CODE_CUSTOMER
import com.neuroid.tracker.events.NID_ORIGIN_CODE_FAIL
import com.neuroid.tracker.events.NID_ORIGIN_CODE_NID
import com.neuroid.tracker.events.NID_ORIGIN_CUSTOMER_SET
import com.neuroid.tracker.events.NID_ORIGIN_NID_SET
import com.neuroid.tracker.events.SET_REGISTERED_USER_ID
import com.neuroid.tracker.events.SET_USER_ID
import com.neuroid.tracker.events.SET_VARIABLE
import com.neuroid.tracker.models.SessionIDOriginResult
import com.neuroid.tracker.utils.NIDLogWrapper

internal class NIDIdentifierService(
    val logger: NIDLogWrapper,
    val neuroID: NeuroID,
    val validationService: NIDValidationService,
) {
    internal fun getOriginResult(
        sessionID: String,
        validID: Boolean,
        userGenerated: Boolean,
    ): SessionIDOriginResult {
        var origin =
            if (userGenerated) {
                NID_ORIGIN_CUSTOMER_SET
            } else {
                NID_ORIGIN_NID_SET
            }

        var originCode =
            if (validID) {
                if (userGenerated) {
                    NID_ORIGIN_CODE_CUSTOMER
                } else {
                    NID_ORIGIN_CODE_NID
                }
            } else {
                NID_ORIGIN_CODE_FAIL
            }
        return SessionIDOriginResult(origin, originCode, sessionID)
    }

    internal fun sendOriginEvent(originResult: SessionIDOriginResult) {
        // sending these as individual items.
        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionIdCode",
            v = originResult.originCode,
        )

        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionIdSource",
            v = originResult.origin,
        )

        neuroID.captureEvent(
            queuedEvent = !NeuroID.isSDKStarted,
            type = SET_VARIABLE,
            key = "sessionId",
            v = originResult.sessionID,
        )
    }

    fun setGenericUserID(
        type: String,
        genericUserId: String,
        userGenerated: Boolean = true,
    ): Boolean {
        try {
            val validID = validationService.validateUserID(genericUserId)
            val originRes =
                getOriginResult(
                    genericUserId,
                    validID = validID,
                    userGenerated = userGenerated,
                )

            sendOriginEvent(originRes)

            if (!validID) {
                return false
            }

            if (NeuroID.isSDKStarted) {
                neuroID.captureEvent(
                    type = type,
                    uid = genericUserId,
                )
            } else {
                neuroID.captureEvent(
                    queuedEvent = true,
                    type = type,
                    uid = genericUserId,
                )
            }

            return true
        } catch (exception: Exception) {
            logger.e(msg = "failure processing user id! $type, $genericUserId $exception")
            return false
        }
    }

    fun getUserID() = neuroID.userID

    fun setUserID(
        userId: String,
        userGenerated: Boolean,
    ): Boolean {
        val validID = setGenericUserID(SET_USER_ID, userId, userGenerated)

        if (!validID) {
            return false
        }

        neuroID.userID = userId
        return true
    }

    fun getRegisteredUserID() = neuroID.registeredUserID

    fun setRegisteredUserID(registeredUserID: String): Boolean {
        if (neuroID.registeredUserID.isNotEmpty() && registeredUserID != neuroID.registeredUserID) {
            neuroID.captureEvent(type = LOG, level = "warn", m = "Multiple Registered User Id Attempts")
            logger.e(msg = "Multiple Registered UserID Attempt: Only 1 Registered UserID can be set per session")
            return false
        }

        val validID =
            setGenericUserID(
                SET_REGISTERED_USER_ID,
                registeredUserID,
            )

        if (!validID) {
            return false
        }

        neuroID.registeredUserID = registeredUserID
        return true
    }
}
