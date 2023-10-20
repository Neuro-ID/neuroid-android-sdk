package com.neuroid.tracker.service

import java.io.BufferedReader
import java.io.InputStreamReader

class NIDAdvKeyService {
    fun getKey(callback: OnKeyCallback, connProvider: HttpConnectionProvider,
               keyMapper: GsonAdvMapper, base64Decoder: Base64Decoder, siteId: String) {
        var retryCount = 0
        while (retryCount < RETRY_MAX) {
            val conn = connProvider.getConnection("$URL/$siteId")
            try {
                when (conn.responseCode) {
                    200, 203 -> {
                        // read data
                        val buffer = StringBuffer()
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        reader.forEachLine { line ->
                            buffer.append(line)
                        }
                        reader.close()
                        // turn json data into adv object
                        val advData = keyMapper.getKey(buffer.toString())
                        // check status ok ,else error!
                        if (advData.status == "OK") {
                            callback.onKeyGotten(
                                base64Decoder.decodeBase64(
                                    keyMapper.getKey(buffer.toString()).key
                                )
                            )
                        } else {
                            callback.onFailure("error: status ${advData.status}", conn.responseCode)
                        }
                        retryCount = RETRY_MAX
                    }

                    else -> {
                        // got response code other than 200 / 203
                        callback.onFailure(
                            "error! response message: ${conn.responseMessage} method: ${conn.requestMethod}",
                            conn.responseCode
                        )
                        retryCount += 1
                    }
                }
            } catch (exception: Exception) {
                var message = "some exception"
                exception.message?.let { errorMessage ->
                    message = errorMessage
                }
                callback.onFailure("error! no response, message: $message", -1)
                retryCount += 1
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        // update the tests if you change the retry count!
        const val RETRY_MAX = 3
        const val URL = "https://receiver.neuro-dev.com/a"
    }
}

interface OnKeyCallback {
    fun onKeyGotten(key: String)
    fun onFailure(message: String, responseCode: Int)
}