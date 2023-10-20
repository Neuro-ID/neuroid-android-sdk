package com.neuroid.tracker

import com.neuroid.tracker.models.AdvancedDeviceKey
import com.neuroid.tracker.utils.Base64Decoder
import com.neuroid.tracker.utils.GsonAdvMapper
import com.neuroid.tracker.utils.HttpConnectionProvider
import com.neuroid.tracker.service.NIDAdvKeyService
import com.neuroid.tracker.service.OnKeyCallback
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import java.net.HttpURLConnection

class NIDHttpAdvDevKeyTest {
    @Test
    fun test_http_200_ok() {
        val callBack = mockk<OnKeyCallback>()
        every{ callBack.onKeyGotten(any()) } just runs
        every{ callBack.onFailure(any(), any()) } just runs
        val conn = mockk<HttpURLConnection>()
        every{ conn.disconnect() } just runs
        every{ conn.responseCode } returns 200
        every{ conn.inputStream } returns ("{\"status\":\"OK\", \"key\":\"g2334asdgawe4535435r=\"}").byteInputStream()
        val connProvider = mockk<HttpConnectionProvider>()
        every { connProvider.getConnection(any()) } returns conn
        val mapper = mockk<GsonAdvMapper>()
        every{mapper.getKey("{\"status\":\"OK\", \"key\":\"g2334asdgawe4535435r=\"}")} returns AdvancedDeviceKey("OK", "g2334asdgawe4535435r=")
        val keyService = NIDAdvKeyService()
        val decoder = mockk<Base64Decoder>()
        every{ decoder.decodeBase64("g2334asdgawe4535435r=") } returns "test_key"
        keyService.getKey(callBack, connProvider, mapper, decoder, "12345")
        verify{ mapper.getKey("{\"status\":\"OK\", \"key\":\"g2334asdgawe4535435r=\"}") }
        verify{ decoder.decodeBase64("g2334asdgawe4535435r=") }
        verify(exactly = 1){ callBack.onKeyGotten("test_key") }
    }

    @Test
    fun test_http_200_not_ok() {
        val callBack = mockk<OnKeyCallback>()
        every{ callBack.onFailure(any(), any()) } just runs
        val conn = mockk<HttpURLConnection>()
        every{ conn.disconnect() } just runs
        every{ conn.responseCode } returns 200
        every{ conn.inputStream } returns ("{\"status\":\"notOK\", \"key\":\"g2334asdgawe4535435r=\"}").byteInputStream()
        val connProvider = mockk<HttpConnectionProvider>()
        every { connProvider.getConnection(any()) } returns conn
        val mapper = mockk<GsonAdvMapper>()
        every{mapper.getKey("{\"status\":\"notOK\", \"key\":\"g2334asdgawe4535435r=\"}")} returns AdvancedDeviceKey("notOK", "g2334asdgawe4535435r=")
        val keyService = NIDAdvKeyService()
        val decoder = mockk<Base64Decoder>()
        keyService.getKey(callBack, connProvider, mapper, decoder, "12345")
        verify{ mapper.getKey("{\"status\":\"notOK\", \"key\":\"g2334asdgawe4535435r=\"}") }
        verify(exactly = 1){ callBack.onFailure("advanced signal not available: status notOK", 200) }
    }

    @Test
    fun test_http_404() {
        val callBack = mockk<OnKeyCallback>()
        every{ callBack.onFailure(any(), any()) } just runs
        val conn = mockk<HttpURLConnection>()
        every{ conn.disconnect() } just runs
        every{ conn.responseCode } returns 404
        every{ conn.responseMessage } returns "big error!"
        every{ conn.requestMethod } returns "get"
        val connProvider = mockk<HttpConnectionProvider>()
        every { connProvider.getConnection(any()) } returns conn
        val mapper = mockk<GsonAdvMapper>()
        val keyService = NIDAdvKeyService()
        val decoder = mockk<Base64Decoder>()
        keyService.getKey(callBack, connProvider, mapper, decoder, "12345")
        verify(exactly = 3){ callBack.onFailure("error! response message: big error! method: get", 404) }
    }

    @Test
    fun test_http_no_response_with_message() {
        val callBack = mockk<OnKeyCallback>()
        every{ callBack.onFailure(any(), any()) } just runs
        val conn = mockk<HttpURLConnection>()
        every{ conn.disconnect() } just runs
        val exception = mockk<Exception>()
        every{ exception.message } returns "big stack trace"
        every{ conn.responseCode} throws exception
        val connProvider = mockk<HttpConnectionProvider>()
        every { connProvider.getConnection(any()) } returns conn
        val mapper = mockk<GsonAdvMapper>()
        val keyService = NIDAdvKeyService()
        val decoder = mockk<Base64Decoder>()
        keyService.getKey(callBack, connProvider, mapper, decoder, "12345")
        verify(exactly = 3){ callBack.onFailure("error! no response, message: big stack trace", -1) }
    }

    @Test
    fun test_http_no_response_no_message() {
        val callBack = mockk<OnKeyCallback>()
        every{ callBack.onFailure(any(), any()) } just runs
        val conn = mockk<HttpURLConnection>()
        every{ conn.disconnect() } just runs
        val exception = mockk<Exception>()
        every{ exception.message } returns null
        every{ conn.responseCode} throws exception
        val connProvider = mockk<HttpConnectionProvider>()
        every { connProvider.getConnection(any()) } returns conn
        val mapper = mockk<GsonAdvMapper>()
        val keyService = NIDAdvKeyService()
        val decoder = mockk<Base64Decoder>()
        keyService.getKey(callBack, connProvider, mapper, decoder, "12345")
        verify(exactly = 3){ callBack.onFailure("error! no response, message: some exception", -1) }
    }

}