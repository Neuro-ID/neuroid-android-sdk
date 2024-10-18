package com.neuroid.tracker.service

import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.getRetroFitInstance
import okhttp3.RequestBody

interface HttpService {
    fun sendEvents(
        body: RequestBody,
        key: String,
        responseCallback: NIDResponseCallBack<Any>,
    )

    fun getConfig(
        key: String,
        responseCallback: NIDResponseCallBack<NIDRemoteConfig>,
    )
}

class NIDHttpService(
    collectionEndpoint: String,
    configEndpoint: String,
    logger: NIDLogWrapper,
    collectionTimeout: Long = 10,
    configTimeout: Long = 10,
) : HttpService, RetrySender() {
    private var collectorAPIService: NIDApiService
    private var configAPIService: NIDApiService

    init {
        // We have to have two different retrofit instances because it requires a
        // `base_url` to work off of and our Collection and Config endpoints are
        // different
        collectorAPIService =
            getRetroFitInstance(
                collectionEndpoint,
                logger,
                NIDApiService::class.java,
                collectionTimeout,
            )

        configAPIService =
            getRetroFitInstance(
                configEndpoint,
                logger,
                NIDApiService::class.java,
                configTimeout,
            )
    }

    override fun sendEvents(
        body: RequestBody,
        key: String,
        responseCallback: NIDResponseCallBack<Any>,
    ) {
        val call = collectorAPIService.sendEvents(body, key)
        retryRequests(call, responseCallback)
    }

    override fun getConfig(
        key: String,
        responseCallback: NIDResponseCallBack<NIDRemoteConfig>,
    ) {
        val call = configAPIService.getConfig(key)
        retryRequests(call, responseCallback)
    }
}
