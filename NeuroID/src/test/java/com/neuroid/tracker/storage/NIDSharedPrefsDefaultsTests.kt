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
    @Test
    fun getSessionID() {
        val context = mockk<Context>()
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "test"
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context)
        assert(t.getSessionID() == "test")
    }

    @Test
    fun getNewSessionID() {
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val uuidProvider = mockk<com.neuroid.tracker.utils.NIDSystemUuidProvider>()
        every {uuidProvider.randomUUID()} returns "new-uuid"
        val t = NIDSharedPrefsDefaults(context, uuidProvider = uuidProvider, dispatcher = Dispatchers.Unconfined)
        t.getNewSessionID()
        verify {
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor.putString("NID_SID_KEY", "new-uuid")
            editor.apply()
        }
    }

    @Test
    fun getClientID_no_stored_cid() {
        val uuidProvider = mockk<com.neuroid.tracker.utils.NIDSystemUuidProvider>()
        every {uuidProvider.randomUUID()} returns "new-uuid"
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns ""
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getClientID()
        verify {
            mockSharedPreferences.getString("NID_CID_GUID_KEY", "")
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor.putString("NID_CID_GUID_KEY", "new-uuid")
            editor.apply()
        }
    }

    @Test
    fun getClientID_stored_cid() {
        val uuidProvider = mockk<com.neuroid.tracker.utils.NIDSystemUuidProvider>()
        every {uuidProvider.randomUUID()} returns "new-uuid"
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "CID"
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getClientID()
        verify(exactly = 0) {
            uuidProvider.randomUUID()
            mockSharedPreferences.edit()
            editor.putString("NID_CID_GUID_KEY", "new-uuid")
            editor.apply()
        }
        verify { mockSharedPreferences.getString("NID_CID_GUID_KEY", "")}
    }

    @Test
    fun getDeviceSalt() {
        val uuidProvider = mockk<com.neuroid.tracker.utils.NIDSystemUuidProvider>()
        every {uuidProvider.randomUUID()} returns "new-uuid"
        val context = mockk<Context>()
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "CID"
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, uuidProvider = uuidProvider)
        t.getDeviceSalt()
        verify { mockSharedPreferences.getString("NID_DEVICE_SALT", "")}
    }

    @Test
    fun putDeviceSalt_test() {
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context)
        t.putDeviceSalt("test-salt")
        verify {
            mockSharedPreferences.edit()
            editor.putString("NID_DEVICE_SALT", "test-salt")
            editor.apply()
        }
    }

    @Test
    fun getDeviceID() {
        val randomGenerator = mockk<com.neuroid.tracker.utils.RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 10.0
        val nidTime = mockk<com.neuroid.tracker.utils.NIDTime>()
        every { nidTime.getCurrentTimeMillis() } returns 1000L
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns ""
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getDeviceID()
        verify {
            mockSharedPreferences.getString("NID_DID_KEY", "")
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor.putString("NID_DID_KEY", "1000.10.0")
            editor.apply()
        }
    }

    @Test
    fun getDeviceID_stored_id() {
        val randomGenerator = mockk<com.neuroid.tracker.utils.RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 10.0
        val nidTime = mockk<com.neuroid.tracker.utils.NIDTime>()
        every { nidTime.getCurrentTimeMillis() } returns 1000L
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getDeviceID()
        verify (exactly = 0) {
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor.putString("NID_DID_KEY", "1000.10.0")
            editor.apply()
        }
        verify { mockSharedPreferences.getString("NID_DID_KEY", "") }
    }

    @Test
    fun getIntermediateID() {
        val randomGenerator = mockk<com.neuroid.tracker.utils.RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 10.0
        val nidTime = mockk<com.neuroid.tracker.utils.NIDTime>()
        every { nidTime.getCurrentTimeMillis() } returns 1000L
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns ""
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getIntermediateID()
        verify {
            mockSharedPreferences.getString("NID_IID_KEY", "")
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor.putString("NID_IID_KEY", "1000.10.0")
            editor.apply()
        }
    }

    @Test
    fun getIntermediateID_no_id() {
        val randomGenerator = mockk<com.neuroid.tracker.utils.RandomGenerator>()
        every { randomGenerator.getRandom(any()) } returns 10.0
        val nidTime = mockk<com.neuroid.tracker.utils.NIDTime>()
        every { nidTime.getCurrentTimeMillis() } returns 1000L
        val context = mockk<Context>()
        val editor = mockk<Editor>()
        every {editor.putString(any(), any())} returns editor
        every {editor.apply()} just runs
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        every {mockSharedPreferences.edit()} returns editor
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val t = NIDSharedPrefsDefaults(context = context, randomGenerator = randomGenerator, nidTime = nidTime)
        t.getIntermediateID()
        verify (exactly = 0) {
            nidTime.getCurrentTimeMillis()
            randomGenerator.getRandom(any())
            mockSharedPreferences.edit()
            editor.putString("NID_IID_KEY", "1000.10.0")
            editor.apply()
        }
        verify { mockSharedPreferences.getString("NID_IID_KEY", "") }
    }

    @Test
    fun getLocale() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getDefaultLocale() } returns "en_US"
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getLocale() == "en_US")
    }

    @Test
    fun getLanguage() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getDefaultLanguage() } returns "en_US"
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getLanguage() == "en_US")
    }

    @Test
    fun getDefaultAgent() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getHttpAgent() } returns "http_agent"
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getUserAgent() == "http_agent")
    }

    @Test
    fun getTimeZone() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getHttpAgent() } returns "http_agent"
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getTimeZone() == 300)
    }

    @Test
    fun getPlatform() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getHttpAgent() } returns "http_agent"
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getPlatform() == "Android")
    }

    @Test
    fun getDisplayWidth() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getDisplayMetricsWidth() } returns 1000
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getDisplayWidth() == 1000)
    }

    @Test
    fun getDisplayHeight() {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every {mockSharedPreferences.getString(any(), any())} returns "gsagdfg"
        val context = mockk<Context>()
        every {context.getSharedPreferences(any(), any())} returns mockSharedPreferences
        val mockedNIDResourcesUtils = mockk<com.neuroid.tracker.utils.NIDResourcesUtils>()
        every { mockedNIDResourcesUtils.getDisplayMetricsHeight() } returns 480
        val t = NIDSharedPrefsDefaults(context = context, resourcesProvider = mockedNIDResourcesUtils)
        assert(t.getDisplayHeight() == 480)
    }

}