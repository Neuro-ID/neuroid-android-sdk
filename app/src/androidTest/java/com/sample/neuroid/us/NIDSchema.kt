package com.sample.neuroid.us

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDHttpService
import com.neuroid.tracker.service.getSendingService
import com.neuroid.tracker.utils.NIDLogWrapper
import org.everit.json.schema.Validator
import org.everit.json.schema.event.*
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
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
        maxEventsCount: Int = 1,
    ) {
        val events: Set<String>
        if (eventType.isNotEmpty() && eventList.isNotEmpty()) {
            events = eventList.filter { it.contains(eventType) }.toSet()
            if (maxEventsCount > 0) {
                assertEquals(
                    eventList.toList().joinToString(",").ifEmpty { "No Events" },
                    maxEventsCount,
                    events.size,
                )
            } else {
                assertTrue(events.isNotEmpty())
            }
        }
    }

    suspend fun validateSchema(eventList: List<NIDEventModel>) {
        val json =
            getJsonData(
                context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
                eventList,
            )
        validateSchema(json)

        // Commenting out because tests shouldn't send traffic to prod. Will put back in once dev url is available
//        val application = ApplicationProvider.getApplicationContext<Application>()
//        getSendingService(
//            "", // PUT ENDPOINT WE WANT TO USE (AKA Dev)
//            NIDLogWrapper(),
//            application
//        ).sendTrackerData(
//            key = "key_live_suj4CX90v0un2k1ufGrbItT5",
//            eventList,
//            object: NIDResponseCallBack {
//                override fun onSuccess(code: Int) {
//                    assertEquals(json, 200, code)
//                }
//                override fun onFailure(code: Int, message: String, isRetry: Boolean) {
//                    assert(false)
//                }
//            }
//        )
    }

    private fun validateSchema(json: String) {
        val rawSchema = JSONObject(readFileWithoutNewLineFromResources("schema.json"))
        val schema =
            SchemaLoader.builder().schemaJson(rawSchema).draftV6Support().build().load().build()
        val validator =
            Validator.builder().withListener(
                object : ValidationListener {
                    override fun combinedSchemaMatch(event: CombinedSchemaMatchEvent?) {
                        Assert.assertTrue(event?.schema.toString(), true)
                        super.combinedSchemaMatch(event)
                    }

                    override fun combinedSchemaMismatch(event: CombinedSchemaMismatchEvent?) {
                        // Assert.fail(event.toString())
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
                },
            ).build()
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

    private fun getInputStreamFromResource(fileName: String) = javaClass.classLoader?.getResourceAsStream(fileName)

    private fun getJsonData(
        context: Context,
        listEvents: List<NIDEventModel>,
    ): String {
        return getSendingService(
            NIDHttpService(
                collectionEndpoint = "",
                configEndpoint = "",
                logger = NIDLogWrapper(),
                // We can't use the config value because it hasn't been called.
                // Might have to recreate once config is retrieved
                collectionTimeout = 5,
                configTimeout = 5,
            ),
            context,
        ).getRequestPayloadJSON(listEvents)
    }
}
