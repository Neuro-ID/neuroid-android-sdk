package com.neuroid.tracker.utils

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class NIDTLSSocketFactory: SSLSocketFactory() {
    private var internalSSLSocketFactory: SSLSocketFactory? = null

    init {
        try {
            val context: SSLContext = SSLContext.getInstance("TLS")
            context.init(null, null, null)
            internalSSLSocketFactory = context.socketFactory
        } catch (exception: Exception) {
            //No op
        }
    }

    override fun getDefaultCipherSuites(): Array<String?>? {
        return internalSSLSocketFactory!!.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String?>? {
        return internalSSLSocketFactory!!.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory!!.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String?, port: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory!!.createSocket(host, port))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int
    ): Socket? {
        return enableTLSOnSocket(
            internalSSLSocketFactory!!.createSocket(
                host,
                port,
                localHost,
                localPort
            )
        )
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress?, port: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory?.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket? {
        return enableTLSOnSocket(
            internalSSLSocketFactory?.createSocket(
                address,
                port,
                localAddress,
                localPort
            )
        )
    }

    private fun enableTLSOnSocket(socket: Socket?): Socket? {
        if (socket != null && socket is SSLSocket) {
            socket.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
        }
        return socket
    }
}