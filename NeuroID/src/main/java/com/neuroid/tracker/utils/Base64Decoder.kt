package com.neuroid.tracker.utils

import android.util.Base64

class Base64Decoder {
    fun decodeBase64(data: String): String = String(Base64.decode(data, Base64.DEFAULT))
}
