package com.neuroid.tracker.service

import com.neuroid.tracker.utils.NIDLogWrapper
import okhttp3.Interceptor
import okhttp3.Response

class LoggerIntercepter(val logger: NIDLogWrapper) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 200) {
            logger.e(
                msg =
                    "http error, ${request.method}, ${request.url}, " +
                        "${response.code}, ${response.cacheResponse?.body}",
            )
        } else {
            logger.d(
                msg =
                    "http ok, ${request.method}, ${request.url}, " +
                        "${response.code}",
            )
        }
        return response
    }
}
