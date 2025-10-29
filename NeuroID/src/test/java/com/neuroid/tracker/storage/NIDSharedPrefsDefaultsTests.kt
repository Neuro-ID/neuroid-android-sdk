package com.neuroid.tracker.storage

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Test

class NIDSharedPrefsDefaultsTests {

    companion object {
        /**
         * Creates a mock Context with SharedPreferences and optional Editor.
         * @param sharedPrefsStringValue The return value for getString() calls
         * @param withEditor If true, includes an editor mock configured for write operations
         * @return Triple of (Context, SharedPreferences, Editor?)
         */
        fun createMockContext(
            sharedPrefsStringValue: String = "gsagdfg",
            withEditor: Boolean = false
        ): Triple<Context, SharedPreferences, Editor?> {
            val context = mockk<Context>()
            val mockSharedPreferences = mockk<SharedPreferences>()
            every { mockSharedPreferences.getString(any(), any()) } returns sharedPrefsStringValue
            every { context.getSharedPreferences(any(), any()) } returns mockSharedPreferences

            val editor = if (withEditor) {
                val ed = mockk<Editor>()
                every { ed.putString(any(), any()) } returns ed
                every { ed.apply() } just runs
                every { mockSharedPreferences.edit() } returns ed
                ed
            } else {
                null
            }

            return Triple(context, mockSharedPreferences, editor)
        }

        /**
         * Creates a mock UUID provider.
         * @param uuid The UUID to return from randomUUID()
         * @return Mocked NIDSystemUuidProvider
         */
        fun createMockUuidProvider(uuid: String = "new-uuid"): com.neuroid.tracker.utils.NIDSystemUuidProvider {
            val uuidProvider = mockk<com.neuroid.tracker.utils.NIDSystemUuidProvider>()
            every { uuidProvider.randomUUID() } returns uuid
            return uuidProvider
        }

        /**
         * Creates mock RandomGenerator and NIDTime instances.
         * @param randomValue The value to return from getRandom()
         * @param timeMillis The time to return from getCurrentTimeMillis()
         * @return Pair of (RandomGenerator, NIDTime)
         */
        fun createMockRandomGeneratorAndTime(
            randomValue: Double = 10.0,
            timeMillis: Long = 1000L
        ): Pair<com.neuroid.tracker.utils.RandomGenerator, com.neuroid.tracker.utils.NIDTime> {
            val randomGenerator = mockk<com.neuroid.tracker.utils.RandomGenerator>()
            every { randomGenerator.getRandom(any()) } returns randomValue
            
            val nidTime = mockk<com.neuroid.tracker.utils.NIDTime>()
            every { nidTime.getCurrentTimeMillis() } returns timeMillis
            
            return Pair(randomGenerator, nidTime)
        }

        /**
         * Creates a mock NIDResourcesUtils with configurable behaviors.
         * Use the returned ResourcesMockBuilder to configure specific return values.
         */
        fun createMockResourcesUtils(): ResourcesMockBuilder {
            return ResourcesMockBuilder()
        }

        /**
         * Builder class for configuring NIDResourcesUtils mock.
         */
        class ResourcesMockBuilder {
            private val mock = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()

            fun withLocale(locale: String): ResourcesMockBuilder {
                every { mock.getDefaultLocale() } returns locale
                return this
            }

            fun withLanguage(language: String): ResourcesMockBuilder {
                every { mock.getDefaultLanguage() } returns language
                return this
            }

            fun withHttpAgent(agent: String): ResourcesMockBuilder {
                every { mock.getHttpAgent() } returns agent
                return this
            }

            fun withDisplayWidth(width: Int): ResourcesMockBuilder {
                every { mock.getDisplayMetricsWidth() } returns width
                return this
            }

            fun withDisplayHeight(height: Int): ResourcesMockBuilder {
                every { mock.getDisplayMetricsHeight() } returns height
                return this
            }

            fun build(): com.neuroid.tracker.utils.NIDResourcesUtils = mock
        }
    }

    @Test
    fun getSessionID() {
        val (context, _, _) = createMockContext(sharedPrefsStringValue = "test")
        val t = NIDSharedPrefsDefaults(context)
        assert(t.getSessionID() == "test")
    }

    @Test
    fun getNewSessionID() {
        val (context, mockSharedPreferences, editor) = createMockContext(withEditor = true)
        val uuidProvider = createMockUuidProvider()
        val t = NIDSharedPrefsDefaults(context, uuidProvider = uuidProvider, dispatcher = Dispatchers.Unconfined)
        t.getNewSessionID()
        verify {
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor?.putString("NID_SID_KEY", "new-uuid")
            editor?.apply()
        }
    }

    @Test
    fun getClientID_no_stored_cid() {
        val uuidProvider = createMockUuidProvider()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getClientID()
        verify {
            mockSharedPreferences.getString("NID_CID_GUID_KEY", "")
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor?.putString("NID_CID_GUID_KEY", "new-uuid")
            editor?.apply()
        }
    }

    @Test
    fun getClientID_stored_cid() {
        val uuidProvider = createMockUuidProvider()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "CID",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getClientID()
        verify(exactly = 0) {
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor?.putString("NID_CID_GUID_KEY", "new-uuid")
            editor?.apply()
        }
        verify { mockSharedPreferences.getString("NID_CID_GUID_KEY", "") }
    }

    @Test
    fun getDeviceSalt() {
        val uuidProvider = createMockUuidProvider()
        val (context, mockSharedPreferences, _) = createMockContext(sharedPrefsStringValue = "CID")
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getDeviceSalt()
        verify { mockSharedPreferences.getString("NID_DEVICE_SALT", "") }
    }

    @Test
    fun putDeviceSalt_test() {
        val (context, mockSharedPreferences, editor) = createMockContext(withEditor = true)
        val t = NIDSharedPrefsDefaults(context = context)
        t.putDeviceSalt("test-salt")
        verify {
            mockSharedPreferences.edit()
            editor?.putString("NID_DEVICE_SALT", "test-salt")
            editor?.apply()
        }
    }

    @Test
    fun getDeviceID() {
        val (randomGenerator, nidTime) = createMockRandomGeneratorAndTime()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getDeviceID()
        verify {
            mockSharedPreferences.getString("NID_DID_KEY", "")
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor?.putString("NID_DID_KEY", "1000.10.0")
            editor?.apply()
        }
    }

    @Test
    fun getDeviceID_stored_id() {
        val (randomGenerator, nidTime) = createMockRandomGeneratorAndTime()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "gsagdfg",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getDeviceID()
        verify(exactly = 0) {
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor?.putString("NID_DID_KEY", "1000.10.0")
            editor?.apply()
        }
        verify { mockSharedPreferences.getString("NID_DID_KEY", "") }
    }

    @Test
    fun getIntermediateID() {
        val (randomGenerator, nidTime) = createMockRandomGeneratorAndTime()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getIntermediateID()
        verify {
            mockSharedPreferences.getString("NID_IID_KEY", "")
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor?.putString("NID_IID_KEY", "1000.10.0")
            editor?.apply()
        }
    }

    @Test
    fun getIntermediateID_no_id() {
        val (randomGenerator, nidTime) = createMockRandomGeneratorAndTime()
        val (context, mockSharedPreferences, editor) = createMockContext(
            sharedPrefsStringValue = "gsagdfg",
            withEditor = true
        )
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getIntermediateID()
        verify(exactly = 0) {
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor?.putString("NID_IID_KEY", "1000.10.0")
            editor?.apply()
        }
        verify { mockSharedPreferences.getString("NID_IID_KEY", "") }
    }

    @Test
    fun getLocale() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withLocale("en_US")
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getLocale() == "en_US")
    }

    @Test
    fun getLanguage() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withLanguage("en_US")
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getLanguage() == "en_US")
    }

    @Test
    fun getDefaultAgent() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withHttpAgent("http_agent")
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getUserAgent() == "http_agent")
    }

    @Test
    fun getTimeZone() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withHttpAgent("http_agent")
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getTimeZone() == 300)
    }

    @Test
    fun getPlatform() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withHttpAgent("http_agent")
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getPlatform() == "Android")
    }

    @Test
    fun getDisplayWidth() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withDisplayWidth(1000)
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getDisplayWidth() == 1000)
    }

    @Test
    fun getDisplayHeight() {
        val (context, _, _) = createMockContext()
        val mockedNIDResourcesUtils = createMockResourcesUtils()
            .withDisplayHeight(480)
            .build()
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getDisplayHeight() == 480)
    }

}