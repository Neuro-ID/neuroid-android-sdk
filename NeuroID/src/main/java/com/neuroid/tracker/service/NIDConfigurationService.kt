package com.neuroid.tracker.service

import com.neuroid.tracker.models.NIDRemoteConfig

class NIDConfigurationService(
    apiService: NIDApiService,
    remoteConfigListener: OnRemoteConfigReceivedListener,
    key: String,
) : RetrySender() {
    private val responseCallback =
        object : NIDResponseCallBack {
            override fun <T> onSuccess(
                code: Int,
                response: T,
            ) {
                remoteConfigListener.onRemoteConfigReceived(response as NIDRemoteConfig)
            }

            override fun onFailure(
                code: Int,
                message: String,
                isRetry: Boolean,
            ) {
                remoteConfigListener.onRemoteConfigReceivedFailed(
                    "Unable to retrieve the remote configuration for key $key. The default values will be used instead",
                )
            }
        }

    init {
        val call = apiService.getConfig(key)
        retryRequests(call, responseCallback)
    }
}

interface OnRemoteConfigReceivedListener {
    fun onRemoteConfigReceived(remoteConfig: NIDRemoteConfig)

    fun onRemoteConfigReceivedFailed(errorMessage: String)
}
