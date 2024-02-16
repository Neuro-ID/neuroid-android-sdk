package com.neuroid.tracker.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface NIDAdvancedDeviceApiService {
    @GET("/a/{key}")
    fun getADVKey(@Path("key") key: String): Call<ResponseBody>
}