package com.neuroid.tracker.service

interface NIDResponseCallBack {
    fun <T> onSuccess(code: Int, response: T)

    fun onFailure(
        code: Int,
        message: String,
        isRetry: Boolean,
    )
} 
