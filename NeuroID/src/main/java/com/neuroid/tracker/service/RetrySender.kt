package com.neuroid.tracker.service

import com.neuroid.tracker.models.NIDResponseCallBack
import retrofit2.Call

abstract class RetrySender {
    companion object {
        // if you change the retry count, please update the test!
        const val RETRY_COUNT = 3
        const val HTTP_SUCCESS = 200
    }

    fun <T, R> retryRequests(
        call: Call<T>,
        responseCallback: NIDResponseCallBack<R>,
    ) {
        try {
            var retryCount = 0
            while (retryCount < RETRY_COUNT) {
                // retain the existing call and always execute on clones of it so we can retry when
                // there is a failure!
                val retryCall = call.clone()
                val response = retryCall.execute()
                // only allow 200 codes to succeed, everything else is failure, 204 is a failure
                // which is weird!
                if (response.code() == HTTP_SUCCESS) {
                    responseCallback.onSuccess(response.code(), response.body() as R)
                    break
                } else {
                    // response code is not 200, retry these up to RETRY_COUNT times
                    retryCount++
                    responseCallback.onFailure(
                        response.code(),
                        response.message(),
                        retryCount < RETRY_COUNT,
                    )
                }
            }
        } catch (e: Exception) {
            var errorMessage = "no error message available"
            e.message?.let {
                errorMessage = it
            }
            responseCallback.onFailure(-1, errorMessage, false)
        }
    }
}
