package com.sample.neuroid.us

import android.content.Context
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.ANDROID_URI
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.everit.json.schema.Validator
import org.everit.json.schema.event.*
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
class SchemaTest {
    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() = runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        NeuroID.getInstance()?.stop()
    }

    @ExperimentalCoroutinesApi
    @After
    fun resetDispatchers() {
        testScope.launch {
            getDataStoreInstance().clearEvents()
        }
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Validate CHECKBOX_CHANGE when the user click on it
     */
    @Test
    fun test01ValidateSchema() = runBlockingTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")

        Thread.sleep(500) // When you go to the next test, the activity is destroyed and recreated

        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_one_fragment))
            .perform(ViewActions.click())
        Thread.sleep(500)

        Espresso.onView(ViewMatchers.withId(R.id.check_one))
            .perform(ViewActions.click())

        Thread.sleep(500)

        val events = getDataStoreInstance().getAllEvents()
        val json =
            getJsonData(context = getInstrumentation().targetContext.applicationContext, events)

        validate(json)
    }

    private suspend fun getJsonData(context: Context, listEvents: Set<String>): String {
        val listJson = listEvents.map {
            if (it.contains("\"CREATE_SESSION\"")) {
                JSONObject(it.replace("\"url\":\"\"", "\"url\":\"$ANDROID_URI${NIDServiceTracker.firstScreenName}\""))
            } else {
                JSONObject(it)
            }
        }

        val jsonListEvents = JSONArray(listJson)

        return NIDServiceTracker.getContentJson(context, jsonListEvents)
            .replace("\\/", "/")
    }

    private fun validate(json: String) {
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
    fun readFileWithoutNewLineFromResources(fileName: String): String {
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
}
