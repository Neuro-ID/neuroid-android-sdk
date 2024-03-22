package com.sample.neuroid.us

import android.location.LocationListener
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.storage.getTestingDataStoreInstance
import com.neuroid.tracker.utils.CoroutineScopeAdapter
import com.neuroid.tracker.utils.LocationListenerCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import java.io.StringReader

/**
 * moved the MockServer setup and assertion to this abstract class. the tests will extend this.
 * done to consolidate this code to one location for easier maintenance.
 */
abstract class MockServerTest {

    private val server = MockWebServer()

    private var booleanIsFound = false

    @Before
    fun stopSendEventsToServer() = runTest {
        server.start()
        val url = server.url("/c/").toString()
        NeuroID.getInstance()?.setTestURL(url)
        server.enqueue(MockResponse().setBody("").setResponseCode(200))

        NeuroID.getInstance()?.isStopped()?.let {
            if (it) {
                NeuroID.getInstance()?.start()
            }
        }
        delay(500)
    }

    @After
    fun resetDispatchers() = runTest {
        NeuroID.getInstance()?.getTestingDataStoreInstance()?.clearEvents()
        server.shutdown()
    }

    /**
     * recursively process each item in the JSON stream, stop processing when we find the
     * event we are looking for or when we reach the end of the stream.
     */
    private fun processJSONObject(reader: JsonReader, eventType: String) {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                processJSONObject(reader, eventType)
            }
            JsonToken.END_ARRAY -> {
                reader.endArray()
                processJSONObject(reader, eventType)
            }
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                processJSONObject(reader, eventType)
            }
            JsonToken.END_OBJECT -> {
                reader.endObject()
                processJSONObject(reader, eventType)
            }
            JsonToken.NAME ->  {
                val t = reader.nextName()
                if (t == eventType) {
                    booleanIsFound = true
                } else {
                    processJSONObject(reader, eventType)
                }
            }
            JsonToken.STRING -> {
                val t = reader.nextString()
                if (t == eventType) {
                    booleanIsFound = true
                } else {
                    processJSONObject(reader, eventType)
                }
            }
            JsonToken.NUMBER -> {
                val t = reader.nextString()
                if (t == eventType) {
                    booleanIsFound = true
                } else {
                    processJSONObject(reader, eventType)
                }
            }
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
                processJSONObject(reader, eventType)
            }
            JsonToken.NULL -> {
                reader.nextNull()
                processJSONObject(reader, eventType)
            }
            JsonToken.END_DOCUMENT -> {
            }
        }
    }

    /**
     * switched from a DOM model to a stream to reduce memory usage when parsing the
     * JSON response. so far this is making the tests more reliable locally.
     */
    fun assertRequestBodyContains(eventType:String) {
        var request = server.requestCount
        booleanIsFound = false
        if (request > 0) {
            for (i in 0 until request) {
                var req = server.takeRequest()
                val body = req.body.readUtf8()
                val gson = GsonBuilder()
                    .registerTypeAdapter(LocationListener::class.java, LocationListenerCreator())
                    .registerTypeAdapter(CoroutineScope::class.java, CoroutineScopeAdapter())
                    .create()
                val reader = gson.newJsonReader(StringReader(body))
                reader?.let {
                    processJSONObject(it, eventType)
                }
            }

            if (booleanIsFound) {
                assert(true) {
                    "Found the event man!"
                }
            } else {
                assert(false) {
                    "Failed to find event man!"
                }
            }
        }
    }
}