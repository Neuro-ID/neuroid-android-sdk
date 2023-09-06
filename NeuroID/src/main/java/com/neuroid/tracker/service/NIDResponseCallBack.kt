package com.neuroid.tracker.service

interface NIDResponseCallBack {
    fun onSuccess(code: Int)
    fun onFailure(code: Int, message: String, isRetry: Boolean)
}