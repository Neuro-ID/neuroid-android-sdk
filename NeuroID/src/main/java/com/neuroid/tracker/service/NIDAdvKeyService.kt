package com.neuroid.tracker.service

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import android.util.Base64

class NIDAdvKeyService {
    fun getKey(callback: OnKeyCallback, conn: HttpURLConnection, keyMapper: GsonAdvMapper) {
        try {
            when (conn.responseCode) {
                200 -> {
                    val buffer = StringBuffer()
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    reader.forEachLine { line ->
                        buffer.append(line)
                    }
                    reader.close()
                    val advData = keyMapper.getKey(buffer.toString())
                    if (advData.status == "OK") {
                        callback.onKeyGotten(String(Base64.decode(
                            keyMapper.getKey(buffer.toString()).key, Base64.DEFAULT)))
                    } else {
                        callback.onFailure("error: status not ok, ${advData.status}}")
                    }
                }
                else -> {
                    callback.onFailure("error: response code not 200, code: ${conn.responseCode} on ${conn.responseMessage} ${conn.requestMethod}");
                }
            }
        } catch (exception: Exception) {
            callback.onFailure("error: no response, exception: $exception.stackTraceToString()")
        } finally {
            conn.disconnect()
        }
    }
}

interface OnKeyCallback {
    fun onKeyGotten(key: String)
    fun onFailure(message: String)
}