package com.neuroid.tracker.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.events.OUT_OF_MEMORY
import com.neuroid.tracker.extensions.saveIntegrationHealthEvents
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDVersion
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface NIDSendingService {
    fun sendEvents(
        key: String,
        events: List<NIDEventModel>,
        nidResponseCallback: NIDResponseCallBack
    )


// Request Prep Functions
    fun getRequestPayloadJSON(
        events: List<NIDEventModel>
    ): String



}

/**
 * quiet retry on slow networks will occur auto-magically by OKHttp and is explained here:
 * https://medium.com/inloopx/okhttp-is-quietly-retrying-requests-is-your-api-ready-19489ef35ace
 * and here:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/retry-on-connection-failure/
 * we retry requests that connect and come back with bad response codes.
 */
class NIDEventSender(private var apiService: NIDApiService, private val context:Context):NIDSendingService {
    companion object {
        // if you change the retry count, please update the test!
        const val RETRY_COUNT = 3
        const val HTTP_SUCCESS = 200
    }

    // a static payload to send if OOM occurs
    private var oomPayload = ""

    init {
        initializeStaticPayload()
    }

    /**
     * Create a payload that can be used in the event of an OOM error without additional allocations.
     */
    private fun initializeStaticPayload() {
        if (oomPayload.isEmpty()) {
            oomPayload = getRequestPayloadJSON(
                listOf(
                    NIDEventModel(
                        type = OUT_OF_MEMORY,
                        ts = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    override fun sendEvents(key: String, events: List<NIDEventModel>,  responseCallback: NIDResponseCallBack) {
        var data = ""
        try {
            if(events.isEmpty()) {
                // nothing to send
                return
            }

            data = getRequestPayloadJSON(events)

            NIDLog.d("NeuroID", "payload: ${events.size} events; ${data.length} bytes")
            NeuroID.getInstance()?.saveIntegrationHealthEvents()
        } catch (exception: OutOfMemoryError) {
            // make a best effort attempt to continue and send an out of memory event
            data = oomPayload
        }

        val requestBody = data.toRequestBody("application/JSON".toMediaTypeOrNull())
        val call = apiService.sendEvents(requestBody, key)
        retryRequests(call, responseCallback)
    }

    override fun getRequestPayloadJSON(
        events: List<NIDEventModel>
    ): String {
        val sharedDefaults = NIDSharedPrefsDefaults(context)

        val userID:String? = if(NeuroID.getInstance()?.getUserID() != null) {
            NeuroID.getInstance()?.getUserID()
        } else {
            null
        }
        val registeredUserID:String? = if(NeuroID.getInstance()?.getRegisteredUserID() != null) {
            NeuroID.getInstance()?.getRegisteredUserID()
        } else {
            null
        }

        val jsonBody = mapOf(
            "siteId" to NeuroID.siteID,
            "userId" to userID,
            "clientId" to sharedDefaults.getClientId(),
            "identityId" to userID,
            "registeredUserId" to registeredUserID,
            "pageTag" to NeuroID.screenActivityName,
            "pageId" to NeuroID.rndmId,
            "tabId" to NeuroID.rndmId,
            "responseId" to sharedDefaults.generateUniqueHexId(),
            "url" to "$ANDROID_URI${NeuroID.screenActivityName}",
            "jsVersion" to "5.0.0",
            "sdkVersion" to NIDVersion.getSDKVersion(),
            "environment" to NeuroID.environment,
            "jsonEvents" to events
        )

        // using this JSON library (already included) does not escape /
        val gson: Gson = GsonBuilder().create()
        return gson.toJson(jsonBody)
    }

    internal fun retryRequests(
        call: Call<ResponseBody>,
        responseCallback: NIDResponseCallBack
    ){
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
                    responseCallback.onSuccess(response.code())
                    response.body()?.close()
                    break
                } else {
                    // response code is not 200, retry these up to RETRY_COUNT times
                    retryCount ++
                    responseCallback.onFailure(
                        response.code(),
                        response.message(),
                        retryCount < RETRY_COUNT
                    )
                    response.body()?.close()
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

fun getSendingService(endpoint:String, logger: NIDLogWrapper, context:Context):NIDSendingService = NIDEventSender(
    Retrofit.Builder()
        .baseUrl(endpoint)
        .client(
            OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(LoggerIntercepter(logger)).build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NIDApiService::class.java),
    context
)