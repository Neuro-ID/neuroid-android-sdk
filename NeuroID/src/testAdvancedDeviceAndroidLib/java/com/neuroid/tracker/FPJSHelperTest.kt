package com.neuroid.tracker

import com.fingerprintjs.android.fpjs_pro.Error
import com.fingerprintjs.android.fpjs_pro.FingerprintJS
import com.fingerprintjs.android.fpjs_pro.FingerprintJSProResponse
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.FPJSHelper
import com.neuroid.tracker.utils.NIDLogWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class FPJSHelperTest {
    @Test
    fun fpjs_helper_test_has_id_not_expired() {
        val sharedPrefs = mockk<NIDSharedPrefsDefaults>()
        every { sharedPrefs.hasRequestIdExpired() } returns false
        every { sharedPrefs.getCachedRequestId() } returns "572893hjfelsk"
        val logger = mockk<NIDLogWrapper>()
        every { logger.d(any(), any()) } just runs
        val dataStore =mockk<NIDDataStoreManager>()
        every { dataStore.saveEvent(any()) } just runs
        val fpjsClient = mockk<FingerprintJS>()
        val fpjsHelper = FPJSHelper()
        fpjsHelper.createRequestIdEvent(fpjsClient, 0, 3, sharedPrefs, logger, dataStore)
        verify { sharedPrefs.getCachedRequestId() }
        verify { logger.d(msg="Retrieving Request ID for Advanced Device Signals from cache: 572893hjfelsk") }
        verify { dataStore.saveEvent(any())}
    }

    @Test
    fun fpjs_helper_test_success_expired_id_or_no_id_stored() {
        val sharedPrefs = mockk<NIDSharedPrefsDefaults>()
        every {sharedPrefs.hasRequestIdExpired()} returns true
        every {sharedPrefs.cacheRequestId(any())} just runs
        every {sharedPrefs.cacheRequestIdExpirationTimestamp()} just runs
        val logger = mockk<NIDLogWrapper>()
        every {logger.d(any(), any())} just runs
        val dataStore =mockk<NIDDataStoreManager>()
        every {dataStore.saveEvent(any())} just runs
        val fpjsHelper = FPJSHelper()
        fpjsHelper.createRequestIdEvent(fpjsClientFireAPIKeyOK, 0, 3, sharedPrefs, logger, dataStore)
        verify { logger.d(msg="Request ID not found in cache")}
        verify { logger.d(msg="Generating Request ID for Advanced Device Signals: 543sdgsdg") }
        verify { sharedPrefs.cacheRequestId("543sdgsdg") }
        verify { sharedPrefs.cacheRequestIdExpirationTimestamp() }
        verify { dataStore.saveEvent(any())}
    }

    @Test
    fun fpjs_helper_test_success_no_id_stored_wrong_api_key() {
        val sharedPrefs = mockk<NIDSharedPrefsDefaults>()
        every {sharedPrefs.hasRequestIdExpired()} returns true
        val logger = mockk<NIDLogWrapper>()
        every {logger.e(any(), any())} just runs
        every {logger.d(any(), any())} just runs
        val dataStore =mockk<NIDDataStoreManager>()
        every {dataStore.saveEvent(any())} just runs
        val fpjsHelper = FPJSHelper()
        fpjsHelper.createRequestIdEvent(fpjsClientFireWrongAPIKey, 0, 3, sharedPrefs, logger, dataStore)
        verify { logger.d(msg="Error retrieving Advanced Device Signal Request ID:no request id available: 0") }
        verify { logger.d(msg="Error retrieving Advanced Device Signal Request ID:no request id available: 1") }
        verify { logger.d(msg="Error retrieving Advanced Device Signal Request ID:no request id available: 2") }
        verify (exactly = 3){ logger.d(msg="Request ID not found in cache")}
        verify { logger.e(msg="Reached maximum number of retries (3) to get Advanced Device Signal Request ID:no request id available")}
        verify { dataStore.saveEvent(any())}
    }

    private val fpjsClientFireWrongAPIKey = object: FingerprintJS {
        override fun getVisitorId(listener: (FingerprintJSProResponse) -> Unit) {
            println("no op")
        }

        override fun getVisitorId(
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            val response = mockk<Error>()
            every{response.description} returns "no request id available"
            every{response.requestId} returns "none"
            errorListener(response)
        }

        override fun getVisitorId(
            linkedId:String,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }

        override fun getVisitorId(
            tags: Map<String, Any>,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }

        override fun getVisitorId(
            tags: Map<String, Any>,
            linkedId: String,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }
    }

    private val fpjsClientFireAPIKeyOK = object: FingerprintJS {
        override fun getVisitorId(listener: (FingerprintJSProResponse) -> Unit) {
            println("no op")
        }

        override fun getVisitorId(
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            val response = mockk<FingerprintJSProResponse>()
            every {response.requestId} returns "543sdgsdg"
            listener(response)
        }

        override fun getVisitorId(
            linkedId:String,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }

        override fun getVisitorId(
            tags: Map<String, Any>,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }

        override fun getVisitorId(
            tags: Map<String, Any>,
            linkedId: String,
            listener: (FingerprintJSProResponse) -> Unit,
            errorListener: (com.fingerprintjs.android.fpjs_pro.Error) -> Unit
        ) {
            println("no op")
        }
    }

}