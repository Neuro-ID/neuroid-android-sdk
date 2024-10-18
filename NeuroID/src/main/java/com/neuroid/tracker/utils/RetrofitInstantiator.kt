package com.neuroid.tracker.utils

import com.neuroid.tracker.service.LoggerIntercepter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun <T> getRetroFitInstance(
    endpoint: String,
    logger: NIDLogWrapper,
    service: Class<T>,
    timeOut: Long,
): T =
    Retrofit.Builder()
        .baseUrl(endpoint)
        .client(
            OkHttpClient.Builder()
                .readTimeout(timeOut, TimeUnit.SECONDS)
                .connectTimeout(timeOut, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(timeOut, TimeUnit.SECONDS)
                .addInterceptor(LoggerIntercepter(logger)).build(),
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(service)
