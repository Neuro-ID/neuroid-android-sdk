package com.sample.neuroid.us.data.network

import com.sample.neuroid.us.domain.network.ProfileResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface NIDServices {

    @GET("/v4/sites/{my_form}/profiles/{user_id}")
    suspend fun getProfile(
        @Header("API-KEY") apiKey: String,
        @Path("my_form") myForm: String,
        @Path("user_id") userId: String,
    ): ProfileResponse

}

class NIDNetworkHelper @Inject constructor() {
    companion object {
        const val URL = "https://api.neuro-id.com"
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .retryOnConnectionFailure(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)

    val service: NIDServices by lazy {
        Retrofit.Builder()
            .baseUrl(URL)
            .client(okHttpClient.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build().create(NIDServices::class.java)
    }
}
