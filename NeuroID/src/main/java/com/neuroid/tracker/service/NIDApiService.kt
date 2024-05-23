package com.neuroid.tracker.service

import com.neuroid.tracker.models.NIDRemoteConfig
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NIDApiService {
    @POST("/c/{key}")
    fun sendEvents(
        @Body requestBody: RequestBody,
        @Path("key") key: String,
    ): Call<ResponseBody>

    @GET("/mobile/{key}.json")
    fun getConfig(
        @Path("key") key: String,
    ): Call<NIDRemoteConfig>
}
