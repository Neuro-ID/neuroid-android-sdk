package com.neuroid.tracker.service

import retrofit2.Response

interface NIDResponseCallBack {
    fun <T> onSuccess(code: Int, response: T)

    fun onFailure(
        code: Int,
        message: String,
        isRetry: Boolean,
    )
} 
