package com.neuroid.tracker.service

import com.neuroid.tracker.models.ADVKeyFunctionResponse
import com.neuroid.tracker.models.ADVKeyNetworkResponse
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.getRetroFitInstance
import retrofit2.Call

interface ADVNetworkService {
    fun getNIDAdvancedDeviceAccessKey(
        key: String,
        clientID: String,
        linkedSiteID: String,
    ): ADVKeyFunctionResponse
}

class NIDAdvancedDeviceNetworkService(
    private var apiService: NIDAdvancedDeviceApiService,
    private val logger: NIDLogWrapper,
    private val b64Decoder: Base64Decoder = Base64Decoder(),
) : ADVNetworkService {
    companion object {
        const val RETRY_COUNT = 3
        const val TIMEOUT = 2L // 2000L = 2 sec?
        const val HTTP_SUCCESS = 200
    }

    override fun getNIDAdvancedDeviceAccessKey(
        key: String,
        clientID: String,
        linkedSiteID: String,
    ): ADVKeyFunctionResponse {
        val call = apiService.getNIDAdvancedDeviceAccessKey(key, clientID, linkedSiteID)
        return retryRequests(call)
    }

    internal fun retryRequests(call: Call<ADVKeyNetworkResponse>): ADVKeyFunctionResponse {
        try {
            var finalResponse =
                ADVKeyFunctionResponse(
                    "",
                    false,
                    "Failed to retrieve key from NeuroID",
                )

            for (retryCount in 1..RETRY_COUNT) {
                // retain the existing call and always execute on clones of it so we can retry when
                // there is a failure!
                val retryCall = call.clone()
                val response = retryCall.execute()

                // only allow 200 codes to succeed, everything else is failure, 204 is a failure
                // which is weird!
                if (response.code() == HTTP_SUCCESS) {
                    val responseBody = response.body()

                    // Shouldn't be possible but retrofit says body can be null
                    if (responseBody == null) {
                        finalResponse =
                            ADVKeyFunctionResponse(
                                "",
                                false,
                                message = "advanced signal not available: responseCode ${response.code()}",
                            )
                        break
                    }

                    if (responseBody.status != "OK") {
                        finalResponse =
                            ADVKeyFunctionResponse(
                                "",
                                false,
                                message = "advanced signal not available: status ${responseBody.status}",
                            )
                        break
                    }

                    val key =
                        b64Decoder.decodeBase64(
                            responseBody.key,
                        )

                    finalResponse =
                        ADVKeyFunctionResponse(
                            key,
                            true,
                        )
                    break
                } else {
                    // response code is not 200, retry these up to RETRY_COUNT times
                    logger.d(
                        tag = "NeuroID ADV",
                        msg =
                            """
                            Failed to get API key from NeuroID: ${response.message()} - Code: ${response.code()}. Retrying: ${retryCount < RETRY_COUNT}
                            """.replace("\n", "").trim(),
                    )
                    finalResponse =
                        ADVKeyFunctionResponse(
                            "",
                            false,
                            response.message(),
                        )
                }
            }

            return finalResponse
        } catch (e: Exception) {
            var errorMessage = "no error message available"
            e.message?.let {
                errorMessage = it
            }

            return ADVKeyFunctionResponse(
                "",
                false,
                errorMessage,
            )
        }
    }
}

fun getADVNetworkService(
    endpoint: String,
    logger: NIDLogWrapper,
): ADVNetworkService =
    NIDAdvancedDeviceNetworkService(
        getRetroFitInstance(
            endpoint,
            logger,
            NIDAdvancedDeviceApiService::class.java,
            NIDAdvancedDeviceNetworkService.TIMEOUT,
        ),
        logger,
    )
