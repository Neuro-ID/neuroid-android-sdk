package com.neuroid.tracker.service

import com.neuroid.tracker.utils.NIDLogWrapper
import okhttp3.Interceptor
import okhttp3.Response

class LoggerIntercepter(val logger: NIDLogWrapper): Interceptor {

    /**
     * Right now we dump errors to the NIDLogger. In the future we should be sending this to
     * Datadog through an event relay like MParticle. We can later on build some nice
     * funnels using Amplitude and the events we capture (thinking the screen names)!
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 200) {
            logger.e("Neuro ID","http error, ${request.method}, ${request.url}, " +
                    "${response.code}, ${response.cacheResponse?.body}")
        } else {
            logger.d("Neuro ID","http ok, ${request.method}, ${request.url}, " +
                    "${response.code}")
        }
        return response
    }
}