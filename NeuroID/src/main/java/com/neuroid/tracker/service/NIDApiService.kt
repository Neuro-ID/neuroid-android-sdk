package com.neuroid.tracker.service

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface NIDApiService {
    @POST("/c/{key}")
    fun sendEvents(@Body requestBody: RequestBody, @Path("key") key: String): Call<ResponseBody>
}