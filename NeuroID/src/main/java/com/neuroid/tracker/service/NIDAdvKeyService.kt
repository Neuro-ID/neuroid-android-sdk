package com.neuroid.tracker.service

import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.GsonAdvMapper
import com.neuroid.tracker.utils.HttpConnectionProvider
import java.io.BufferedReader
import java.io.InputStreamReader

class NIDAdvKeyService {
    fun getKey(
        callback: OnKeyCallback, 
        connProvider: HttpConnectionProvider,
        keyMapper: GsonAdvMapper, 
        base64Decoder: Base64Decoder, 
        siteKey: String,
        dataStore: NIDDataStoreManager
        ) {
        var retryCount = 0
        while (retryCount < RETRY_MAX) {
            val conn = connProvider.getConnection("$URL/$siteKey")
            try {
                when (conn.responseCode) {
                    200 -> {
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
                            callback.onFailure("advanced signal not available: status ${advData.status}", conn.responseCode)

                            // send error if we don't get status="OK"
                            dataStore.saveEvent(
                                NIDEventModel(
                                    type = LOG,
                                    ts = System.currentTimeMillis(),
                                    level="error",
                                    m="advanced signal not available: status ${advData.status}, conn.responseCode"
                                )
                            )
                        }
                        break
                    }

                    else -> {
                        // got response code other than 200
                        callback.onFailure(
                            "error! response message: ${conn.responseMessage} method: ${conn.requestMethod}",
                            conn.responseCode
                        )
                        Thread.sleep(TIMEOUT)
                        retryCount += 1

                        // send error to server if we hit the max retry limit
                        if (retryCount >= RETRY_MAX) {
                            dataStore.saveEvent(
                                NIDEventModel(
                                    type = LOG,
                                    ts = System.currentTimeMillis(),
                                    level="error",
                                    m="error! response message: ${conn.responseMessage} method: ${conn.requestMethod}"
                                )
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                var message = "some exception"
                exception.message?.let { errorMessage ->
                    message = errorMessage
                }
                callback.onFailure("error! no response, message: $message", -1)
                Thread.sleep(TIMEOUT)

                retryCount += 1

                if (retryCount >= RETRY_MAX) {
                    // send error if we don't get response!
                    dataStore.saveEvent(
                        NIDEventModel(
                            type = LOG,
                            ts = System.currentTimeMillis(),
                            level = "error",
                            m = "error! no response, message: $message, -1"
                        )
                    )
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        // update the tests if you change the retry count!
        const val RETRY_MAX = 3
        const val URL = "https://receiver.neuroid.cloud/a"
        const val TIMEOUT = 2000L
    }
}

interface OnKeyCallback {
    fun onKeyGotten(key: String)
    fun onFailure(message: String, responseCode: Int)
}