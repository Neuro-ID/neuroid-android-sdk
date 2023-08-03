package com.sample.neuroid.us

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.service.NIDServiceTracker
import org.everit.json.schema.Validator
import org.everit.json.schema.event.*
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class NIDSchema {

    /**
     * if validateEvent == false, only schema validation is done.
     */
    suspend fun validateEvents(
        eventList: Set<String>,
        eventType: String = "",
        maxEventsCount: Int = 1
    ) {
        val events: Set<String>
        if (eventType.isNotEmpty() && eventList.isNotEmpty()) {
            events = eventList.filter { it.contains(eventType) }.toSet()
            if (maxEventsCount > 0) {
                assertEquals(
                    eventList.toList().joinToString(",").ifEmpty { "No Events" },
                    maxEventsCount,
                    events.size
                )
            } else {
                assertTrue(events.isNotEmpty())
            }
        }
    }

    suspend fun validateSchema(
        eventList: Set<String>,
    ) {
        val json =
            getJsonData(
                context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
                eventList
            )
        validateSchema(json)

        val application = ApplicationProvider.getApplicationContext<Application>()
        val typeResponse = NIDServiceTracker.sendEventToServer(
            "key_live_suj4CX90v0un2k1ufGrbItT5",
            NeuroID.ENDPOINT_PRODUCTION,
            application,
            eventList
        )
        assertEquals(json, 200, typeResponse.first)
    }

    private fun validateSchema(json: String) {
        val rawSchema = JSONObject(readFileWithoutNewLineFromResources("schema.json"))
        val schema =
            SchemaLoader.builder().schemaJson(rawSchema).draftV6Support().build().load().build()
        val validator = Validator.builder().withListener(object : ValidationListener {
            override fun combinedSchemaMatch(event: CombinedSchemaMatchEvent?) {
                Assert.assertTrue(event?.schema.toString(), true)
                super.combinedSchemaMatch(event)
            }

            override fun combinedSchemaMismatch(event: CombinedSchemaMismatchEvent?) {
                //Assert.fail(event.toString())
                Log.e("Fail", event?.toJSON(true, true).toString())
                super.combinedSchemaMismatch(event)
            }

            override fun schemaReferenced(event: SchemaReferencedEvent?) {
                Assert.assertTrue(event?.schema.toString(), true)
                super.schemaReferenced(event)
            }

            override fun ifSchemaMatch(event: ConditionalSchemaMatchEvent?) {
                Assert.assertTrue(event?.schema.toString(), true)
                super.ifSchemaMatch(event)
            }

            override fun ifSchemaMismatch(event: ConditionalSchemaMismatchEvent?) {
                Log.e("Fail", event?.toJSON(true, true).toString())
                super.ifSchemaMismatch(event)
            }

            override fun thenSchemaMatch(event: ConditionalSchemaMatchEvent?) {
                Assert.assertTrue(event?.schema.toString(), true)
                super.thenSchemaMatch(event)
            }

            override fun thenSchemaMismatch(event: ConditionalSchemaMismatchEvent?) {
                Log.e("Fail", event?.toJSON(true, true).toString())
                super.thenSchemaMismatch(event)
            }

            override fun elseSchemaMatch(event: ConditionalSchemaMatchEvent?) {
                Assert.assertTrue(event?.schema.toString(), true)
                super.elseSchemaMatch(event)
            }

            override fun elseSchemaMismatch(event: ConditionalSchemaMismatchEvent?) {
                Log.e("Fail", event?.toJSON(true, true).toString())
                super.elseSchemaMismatch(event)
            }
        }).build()
        validator.performValidation(schema, JSONObject(json))
    }

    @Throws(IOException::class)
    private fun readFileWithoutNewLineFromResources(fileName: String): String {
        var inputStream: InputStream? = null
        try {
            inputStream = getInputStreamFromResource(fileName)
            val builder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(inputStream))

            var str: String? = reader.readLine()
            while (str != null) {
                builder.append(str)
                str = reader.readLine()
            }
            return builder.toString()
        } finally {
            inputStream?.close()
        }
    }

    private fun getInputStreamFromResource(fileName: String) =
        javaClass.classLoader?.getResourceAsStream(fileName)

    private suspend fun getJsonData(context: Context, listEvents: Set<String>): String {
        val listJson = listEvents.map {
            if (it.contains("\"CREATE_SESSION\"")) {
                JSONObject(
                    it.replace(
                        "\"url\":\"\"",
                        "\"url\":\"$ANDROID_URI${NIDServiceTracker.firstScreenName}\""
                    )
                )
            } else {
                JSONObject(it)
            }
        }

        val jsonListEvents = JSONArray(listJson)

        return NIDServiceTracker.getContentJson(context, jsonListEvents)
            .replace("\\/", "/")
    }
}