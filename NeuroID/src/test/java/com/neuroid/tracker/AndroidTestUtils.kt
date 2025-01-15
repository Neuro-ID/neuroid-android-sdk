package com.neuroid.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.location.LocationManager
import android.view.View
import android.view.ViewGroup
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.models.NIDResponseCallBack
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.models.NIDTouchModel
import com.neuroid.tracker.service.ConfigService
import com.neuroid.tracker.service.HttpService
import com.neuroid.tracker.service.LocationService
import com.neuroid.tracker.service.NIDCallActivityListener
import com.neuroid.tracker.service.NIDConfigService
import com.neuroid.tracker.service.NIDHttpService
import com.neuroid.tracker.service.NIDIdentifierService
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDSamplingService
import com.neuroid.tracker.service.NIDSessionService
import com.neuroid.tracker.service.NIDValidationService
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.storage.NIDSharedPrefsDefaults
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDMetaData
import com.neuroid.tracker.utils.RandomGenerator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import java.util.Calendar

internal fun getMockedNeuroID(
    forceStart: Boolean = false,
    shouldMockApplication: Boolean = false,
    mockDataStore: NIDDataStoreManager = getMockedDataStore(),
    mockJobServiceManager: NIDJobServiceManager = getMockedNIDJobServiceManager(),
    mockLocationService: LocationService = getMockedLocationService(),
    mockCallActivityListener: NIDCallActivityListener = getMockedCallActivityListener(),
    mockSessionService: NIDSessionService = getMockedSessionService(),
): NeuroID {
    val nidMock = mockk<NeuroID>()

    every { nidMock.dispatcher } returns Dispatchers.Unconfined
    every { nidMock.dispatcher = any() } just runs

    every { nidMock.timestamp } returns 0L
    every { nidMock.timestamp = any() } just runs

    every { nidMock.clientKey } returns ""
    every { nidMock.clientKey = any() } just runs

    every { nidMock.sessionID } returns ""
    every { nidMock.sessionID = any() } just runs

    every { nidMock.clientID } returns ""
    every { nidMock.clientID = any() } just runs

    every { nidMock.userID } returns ""
    every { nidMock.userID = any() } just runs

    every { nidMock.registeredUserID } returns ""
    every { nidMock.registeredUserID = any() } just runs

    every { nidMock.linkedSiteID } returns ""
    every { nidMock.linkedSiteID = any() } just runs
    every { nidMock.addLinkedSiteID(any()) } just runs

    every { nidMock.isRN } returns false
    every { nidMock.isRN = any() } just runs

    every { nidMock.forceStart } returns forceStart
    every { nidMock.shouldForceStart() } returns forceStart

    every { nidMock.metaData?.getLastKnownLocation(any(), any(), any()) } returns Unit

    every { nidMock.checkThenCaptureAdvancedDevice() } just runs
    every { nidMock.captureApplicationMetaData() } just runs

    every { nidMock.pauseCollectionJob } returns null
    every { nidMock.pauseCollectionJob = any() } just runs

    every { nidMock.dataStore } returns mockDataStore
    every { nidMock.nidJobServiceManager } returns mockJobServiceManager
    every { nidMock.locationService } returns mockLocationService
    every { nidMock.nidCallActivityListener } returns mockCallActivityListener
    every { nidMock.sessionService } returns mockSessionService

    every { nidMock.setupListeners() } just runs

    if (shouldMockApplication) {
        every { nidMock.application } returns getMockedApplication()
        every { nidMock.getApplicationContext() } returns getMockedApplication()
    }

    every {
        nidMock.captureEvent(
            queuedEvent = any(),
            type = any(),
            ts = any(),
            attrs = any(),
            tg = any(),
            tgs = any(),
            touches = any(),
            key = any(),
            gyro = any(),
            accel = any(),
            v = any(),
            hv = any(),
            en = any(),
            etn = any(),
            ec = any(),
            et = any(),
            eid = any(),
            ct = any(),
            sm = any(),
            pd = any(),
            x = any(),
            y = any(),
            w = any(),
            h = any(),
            sw = any(),
            sh = any(),
            f = any(),
            lsid = any(),
            sid = any(),
            siteId = any(),
            cid = any(),
            did = any(),
            iid = any(),
            loc = any(),
            ua = any(),
            tzo = any(),
            lng = any(),
            ce = any(),
            je = any(),
            ol = any(),
            p = any(),
            dnt = any(),
            tch = any(),
            url = any(),
            ns = any(),
            jsl = any(),
            jsv = any(),
            uid = any(),
            o = any(),
            rts = any(),
            metadata = any(),
            rid = any(),
            m = any(),
            level = any(),
            c = any(),
            isWifi = any(),
            isConnected = any(),
            cp = any(),
            l = any(),
            synthetic = any(),
        )
    } just runs

    return nidMock
}

internal fun getMockedApplication(): Application {
    val mockedSharedPreferencesEditor = mockk<SharedPreferences.Editor>()

    every { mockedSharedPreferencesEditor.apply() } just runs
    every { mockedSharedPreferencesEditor.putString(any(), any()) } returns mockedSharedPreferencesEditor

    val mockedSharedPreferences = mockk<SharedPreferences>()

    every { mockedSharedPreferences.edit() } returns mockedSharedPreferencesEditor
    every { mockedSharedPreferences.getString(any(), any()) } returns ""

    val mockedContext = mockk<Context>()

    every {
        mockedContext.getSharedPreferences(any(), any())
    } returns mockedSharedPreferences

    val mockedApplication = mockk<Application>()

    every { mockedApplication.applicationContext } answers {
        mockedContext
    }

    every { mockedApplication.getSystemService(Context.LOCATION_SERVICE) } returns mockk<LocationManager>()

    every { mockedApplication.getSharedPreferences(any(), any()) } returns mockedSharedPreferences

    return mockedApplication
}

internal fun getMockedLogger(): NIDLogWrapper {
    val logger = mockk<NIDLogWrapper>()
    every { logger.d(any(), any()) } just runs
    every { logger.i(any(), any()) } just runs
    every { logger.w(any(), any()) } just runs
    every { logger.e(any(), any()) } just runs
    return logger
}

internal fun getMockedDataStore(): NIDDataStoreManager {
    val mockDataStore = mockk<NIDDataStoreManager>()

    every { mockDataStore.saveAndClearAllQueuedEvents() } just runs
    every { mockDataStore.clearEvents() } just runs
    every { mockDataStore.isFullBuffer() } returns false

    return mockDataStore
}

internal fun getMockedNIDJobServiceManager(isStopped: Boolean = true): NIDJobServiceManager {
    val mockedNIDJobServiceManager = mockk<NIDJobServiceManager>()

    every { mockedNIDJobServiceManager.isSetup } returns false
    every { mockedNIDJobServiceManager.startJob(any(), any()) } just runs
    every { mockedNIDJobServiceManager.isStopped() } returns isStopped
    every { mockedNIDJobServiceManager.stopJob() } just runs
    every { mockedNIDJobServiceManager.restart() } just runs

    coEvery {
        mockedNIDJobServiceManager.sendEvents(any())
    } just runs

    return mockedNIDJobServiceManager
}

internal fun getMockedRegistrationIdentificationHelper(): RegistrationIdentificationHelper {
    val mocked = mockk<RegistrationIdentificationHelper>()
    every { mocked.registerTargetFromScreen(any(), any(), any(), any(), any()) } just runs
    every { mocked.registerWindowListeners(any()) } just runs
    return mocked
}

internal fun getMockedActivity(): Activity {
    val mockedConfiguration = mockk<Configuration>()
    mockedConfiguration.orientation = 0

    val mockedResources = mockk<Resources>()
    every { mockedResources.configuration } returns mockedConfiguration

    val mockedActivity = mockk<Activity>()
    every { mockedActivity.resources } returns mockedResources
    return mockedActivity
}

internal fun getMockedConfigService(): ConfigService {
    val mockedConfigService = mockk<NIDConfigService>()
    every { mockedConfigService.configCache } returns NIDRemoteConfig()
    every {
        mockedConfigService.retrieveOrRefreshCache()
    } just runs

    return mockedConfigService
}

internal fun getMockSampleService(
    clockTimeInMS: Long,
    randomNumber: Double,
    shouldSample: Boolean = true,
    logger: NIDLogWrapper = getMockedLogger(),
    configService: ConfigService = getMockedConfigService(),
): NIDSamplingService {
    mockkStatic(Calendar::class)
    every { Calendar.getInstance().timeInMillis } returns clockTimeInMS
    // cannot mock Math (FA-Q GOOGLE!!!!) so we wrap it in a helper class that can be mocked
    val randomGenerator = mockk<RandomGenerator>()
    every { randomGenerator.getRandom(any()) } returns randomNumber

    val mockedSampleService = mockk<NIDSamplingService>()

    every { mockedSampleService.isSessionFlowSampled() } returns shouldSample
    every { mockedSampleService.updateIsSampledStatus(any()) } just runs

    every { mockedSampleService.logger } returns logger
    every { mockedSampleService.configService } returns configService

    return mockedSampleService
}

internal fun getMockedLocationService(): LocationService {
    val mockedLocationService = mockk<LocationService>()

    every { mockedLocationService.setupLocationCoroutine(any()) } just runs
    every { mockedLocationService.shutdownLocationCoroutine(any()) } just runs

    return mockedLocationService
}

internal fun getMockedSharedPreferenceDefaults(): NIDSharedPrefsDefaults {
    val mockedSharedPreferencesDefaults = mockk<NIDSharedPrefsDefaults>()

    every { mockedSharedPreferencesDefaults.getNewSessionID() } returns ""
    every { mockedSharedPreferencesDefaults.getClientID() } returns ""

    every { mockedSharedPreferencesDefaults.getDeviceID() } returns ""
    every { mockedSharedPreferencesDefaults.getIntermediateID() } returns ""
    every { mockedSharedPreferencesDefaults.getLocale() } returns ""
    every { mockedSharedPreferencesDefaults.getUserAgent() } returns ""
    every { mockedSharedPreferencesDefaults.getTimeZone() } returns 0
    every { mockedSharedPreferencesDefaults.getLanguage() } returns ""
    every { mockedSharedPreferencesDefaults.getPlatform() } returns ""
    every { mockedSharedPreferencesDefaults.getDisplayWidth() } returns 0
    every { mockedSharedPreferencesDefaults.getDisplayHeight() } returns 0

    return mockedSharedPreferencesDefaults
}

internal fun getMockedCallActivityListener(): NIDCallActivityListener {
    val mockCallActivityListener = mockk<NIDCallActivityListener>()

    every { mockCallActivityListener.setCallActivityListener(any()) } just runs
    every { mockCallActivityListener.unregisterCallActivityListener(any()) } just runs

    return mockCallActivityListener
}

internal fun getMockedSessionService(): NIDSessionService {
    val mockedSessionService = mockk<NIDSessionService>()

    every { mockedSessionService.pauseCollection(any()) } just runs
    every { mockedSessionService.resumeCollection() } just runs
    every { mockedSessionService.createMobileMetadata() } just runs

    return mockedSessionService
}

internal fun getMockedHTTPService(
    isSuccessful: Boolean = false,
    responseCode: Int = 200,
    responseBodyOrMessage: Any = "",
    isRetry: Boolean = false,
): HttpService {
    val mockedHttpService = mockk<NIDHttpService>()

    every { mockedHttpService.sendEvents(any(), any(), any()) } answers {
        val callback = it.invocation.args[2] as NIDResponseCallBack<Any>
        if (isSuccessful) {
            callback.onSuccess(responseCode, responseBodyOrMessage)
        } else {
            callback.onFailure(
                responseCode,
                responseBodyOrMessage as String,
                isRetry,
            )
        }
    }

    every { mockedHttpService.getConfig(any(), any()) } answers {
        val callback = it.invocation.args[1] as NIDResponseCallBack<Any>
        if (isSuccessful) {
            callback.onSuccess(responseCode, responseBodyOrMessage)
        } else {
            callback.onFailure(
                responseCode,
                responseBodyOrMessage as String,
                isRetry,
            )
        }
    }

    return mockedHttpService
}

internal fun getMockedValidationService(): NIDValidationService {
    val mockedNIDValidationService = mockk<NIDValidationService>()

    every {
        mockedNIDValidationService.validateClientKey(any())
    } returns false

    every {
        mockedNIDValidationService.verifyClientKeyExists(any())
    } returns false

    every {
        mockedNIDValidationService.validateSiteID(any())
    } returns false

    every {
        mockedNIDValidationService.validateUserID(any())
    } returns false

    every {
        mockedNIDValidationService.scrubIdentifier(any())
    } returns "MOCK_SCRUBBED_ID"

    return mockedNIDValidationService
}

internal fun getMockedIdentifierService(): NIDIdentifierService {
    val mockedIdentifierService = mockk<NIDIdentifierService>()

    return mockedIdentifierService
}

internal fun getMockedJob(
    isCompleted: Boolean = false,
    isCancelled: Boolean = false,
): Job {
    val mockedJob = mockk<Job>()

    every {
        mockedJob.isCompleted
    } returns isCompleted

    every {
        mockedJob.isCancelled
    } returns isCancelled

    every { mockedJob.invokeOnCompletion(any()) } returns mockk<DisposableHandle>()

    return mockedJob
}

internal fun getMockedView(child: View): View {
    val view = mockk<ViewGroup>()
    val mockedContext = mockk<Context>()
    every { view.childCount } returns 1
    every { view.getChildAt(0) } returns child
    every { view.contentDescription } returns "view group"
    every { view.context } returns mockedContext

    return view
}

internal fun verifyCaptureEvent(
    mockedNeuroID: NeuroID,
    eventType: String,
    count: Int = 1,
    queuedEvent: Boolean? = null,
    attrs: List<Map<String, Any>>? = null,
    tg: Map<String, Any>? = null,
    tgs: String? = null,
    touches: List<NIDTouchModel>? = null,
    key: String? = null,
    gyro: NIDSensorModel? = null,
    accel: NIDSensorModel? = null,
    v: String? = null,
    hv: String? = null,
    en: String? = null,
    etn: String? = null,
    ec: String? = null,
    et: String? = null,
    eid: String? = null,
    ct: String? = null,
    sm: Int? = null,
    pd: Int? = null,
    x: Float? = null,
    y: Float? = null,
    w: Int? = null,
    h: Int? = null,
    sw: Float? = null,
    sh: Float? = null,
    f: String? = null,
    lsid: String? = null,
    sid: String? = null,
    siteId: String? = null,
    cid: String? = null,
    did: String? = null,
    iid: String? = null,
    loc: String? = null,
    ua: String? = null,
    tzo: Int? = null,
    lng: String? = null,
    ce: Boolean? = null,
    je: Boolean? = null,
    ol: Boolean? = null,
    p: String? = null,
    dnt: Boolean? = null,
    tch: Boolean? = null,
    url: String? = null,
    ns: String? = null,
    jsl: List<String>? = null,
    jsv: String? = null,
    uid: String? = null,
    o: String? = null,
    rts: String? = null,
    metadata: NIDMetaData? = null,
    rid: String? = null,
    m: String? = null,
    level: String? = null,
    c: Boolean? = null,
    isWifi: Boolean? = null,
    isConnected: Boolean? = null,
    cp: String? = null,
    l: Long? = null,
    synthetic: Boolean? = null,
) {
    verify(exactly = count) {
        mockedNeuroID.captureEvent(
            queuedEvent = queuedEvent ?: any(),
            type = eventType,
            ts = any(),
            attrs = attrs ?: any(),
            tg = tg ?: any(),
            tgs = tgs ?: any(),
            touches = touches ?: any(),
            key = key ?: any(),
            gyro = gyro ?: any(),
            accel = accel ?: any(),
            v = v ?: any(),
            hv = hv ?: any(),
            en = en ?: any(),
            etn = etn ?: any(),
            ec = ec ?: any(),
            et = et ?: any(),
            eid = eid ?: any(),
            ct = ct ?: any(),
            sm = sm ?: any(),
            pd = pd ?: any(),
            x = x ?: any(),
            y = y ?: any(),
            w = w ?: any(),
            h = h ?: any(),
            sw = sw ?: any(),
            sh = sh ?: any(),
            f = f ?: any(),
            lsid = lsid ?: any(),
            sid = sid ?: any(),
            siteId = siteId ?: any(),
            cid = cid ?: any(),
            did = did ?: any(),
            iid = iid ?: any(),
            loc = loc ?: any(),
            ua = ua ?: any(),
            tzo = tzo ?: any(),
            lng = lng ?: any(),
            ce = ce ?: any(),
            je = je ?: any(),
            ol = ol ?: any(),
            p = p ?: any(),
            dnt = dnt ?: any(),
            tch = tch ?: any(),
            url = url ?: any(),
            ns = ns ?: any(),
            jsl = jsl ?: any(),
            jsv = jsv ?: any(),
            uid = uid ?: any(),
            o = o ?: any(),
            rts = rts ?: any(),
            metadata = metadata ?: any(),
            rid = rid ?: any(),
            m = m ?: any(),
            level = level ?: any(),
            c = c ?: any(),
            isWifi = isWifi ?: any(),
            isConnected = isConnected ?: any(),
            cp = cp ?: any(),
            l = l ?: any(),
            synthetic = synthetic ?: any(),
        )
    }
}
