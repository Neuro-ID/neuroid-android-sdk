package com.neuroid.tracker.service

import com.neuroid.tracker.models.ADVKeyNetworkResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface NIDAdvancedDeviceApiService {
    @GET("/a/{key}")
    fun getNIDAdvancedDeviceAccessKey(
        @Path("key") key: String,
    ): Call<ADVKeyNetworkResponse>
} 
