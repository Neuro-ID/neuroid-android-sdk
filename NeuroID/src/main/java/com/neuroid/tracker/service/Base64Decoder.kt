package com.neuroid.tracker.service

import android.util.Base64

class Base64Decoder {
    fun decodeBase64(data: String): String {
        return String(Base64.decode(data, Base64.DEFAULT))
    }
}