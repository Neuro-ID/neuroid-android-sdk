package com.neuroid.tracker.service

import java.net.HttpURLConnection
import java.net.URL

class HttpConnectionProvider {
    fun getConnection(url: String): HttpURLConnection {
        val conn: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.readTimeout = 5000
        conn.connectTimeout = 5000
        conn.setRequestProperty(
            "Content-Type",
            "application/json"
        )
        return conn
    }
}