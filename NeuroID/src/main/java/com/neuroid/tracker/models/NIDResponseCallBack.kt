package com.neuroid.tracker.models

interface NIDResponseCallBack<T> {
    fun onSuccess(
        code: Int,
        response: T,
    )

    fun onFailure(
        code: Int,
        message: String,
        isRetry: Boolean,
    )
}
