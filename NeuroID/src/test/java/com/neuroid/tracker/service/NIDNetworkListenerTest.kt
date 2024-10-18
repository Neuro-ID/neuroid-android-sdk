package com.neuroid.tracker

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkInfo
import com.neuroid.tracker.events.NETWORK_STATE
import com.neuroid.tracker.service.NIDNetworkListener
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import java.util.Calendar

class NIDNetworkListenerTest {
    @Test
    fun test_networkListener_wifi_connected_sdk_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            true,
            true,
            true,
            true,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_mobile_connected_sdk_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            true,
            true,
            false,
            true,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_mobile_not_connected_sdk_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            false,
            false,
            false,
            true,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_wifi_not_connected_sdk_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            false,
            false,
            true,
            true,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_wifi_connected_sdk_not_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            true,
            true,
            true,
            false,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_mobile_connected_sdk_not_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            true,
            true,
            false,
            false,
            0,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_mobile_not_connected_sdk_not_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            false,
            false,
            false,
            false,
            1,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_wifi_not_connected_sdk_not_stopped_empty_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            false,
            false,
            true,
            false,
            1,
            0,
            "",
        )
    }

    @Test
    fun test_networkListener_wifi_connected_sdk_stopped_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            true,
            true,
            true,
            true,
            0,
            1,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_mobile_connected_sdk_stopped_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            true,
            true,
            false,
            true,
            0,
            1,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_mobile_not_connected_sdk_stopped_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            false,
            false,
            false,
            true,
            0,
            0,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_wifi_not_connected_sdk_stopped_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            false,
            false,
            true,
            true,
            0,
            0,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_wifi_connected_sdk_not_stopped_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            true,
            true,
            true,
            false,
            0,
            0,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_mobile_connected_sdk_not_stopped_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            true,
            true,
            false,
            false,
            0,
            0,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_mobile_not_connected_sdk_not_stopped_userId() {
        networkListenerTestHarness(
            TYPE_MOBILE,
            false,
            false,
            false,
            false,
            1,
            0,
            "ggdasgasdgg",
        )
    }

    @Test
    fun test_networkListener_wifi_not_connected_sdk_not_stopped_userId() {
        networkListenerTestHarness(
            TYPE_WIFI,
            false,
            false,
            true,
            false,
            1,
            0,
            "ggdasgasdgg",
        )
    }

    fun networkListenerTestHarness(
        connectionType: Int,
        isConnectedOrConnecting: Boolean,
        isConnectedAssert: Boolean,
        isWifiAssert: Boolean,
        isStopped: Boolean,
        pauseCalledCount: Int,
        resumeCalledCount: Int,
        userId: String,
    ) {
        val calendar = mockk<Calendar>()
        every { calendar.timeInMillis } returns 5
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar

        val mockedSessionService = getMockedSessionService()

        val neuroID =
            getMockedNeuroID(
                mockSessionService = mockedSessionService,
            )
        every { neuroID.networkConnectionType = any() } just runs // mock for setting the variable
        every { neuroID.networkConnectionType } returns
            if (isWifiAssert) {
                "wifi"
            } else {
                "cell"
            }
        every { neuroID.getNetworkType(context = any()) } returns
            if (isWifiAssert) {
                "wifi"
            } else {
                "cell"
            }
        every { neuroID.isConnected = any() } just runs
        every { neuroID.isConnected } returns isConnectedOrConnecting
        every { neuroID.isStopped() } returns isStopped

        every { neuroID.userID } returns userId

        val networkInfo = mockk<NetworkInfo>()
        every { networkInfo.type } returns connectionType
        every { networkInfo.isConnectedOrConnecting } returns isConnectedOrConnecting

        val connectivityManager = mockk<ConnectivityManager>()
        every { connectivityManager.activeNetworkInfo } returns networkInfo
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { intent.action } returns ConnectivityManager.CONNECTIVITY_ACTION

        val listener =
            NIDNetworkListener(
                connectivityManager,
                neuroID,
                UnconfinedTestDispatcher(),
                0,
                0,
            )
        listener.onReceive(context, intent)

        verifyCaptureEvent(
            neuroID,
            NETWORK_STATE,
            1,
            isConnected = isConnectedAssert,
            isWifi = isWifiAssert,
            ct =
                if (isWifiAssert) {
                    "wifi"
                } else {
                    "cell"
                },
        )
        verify { neuroID.isConnected = isConnectedAssert }
        verify(exactly = pauseCalledCount) { mockedSessionService.pauseCollection(any()) }
        verify(exactly = resumeCalledCount) { mockedSessionService.resumeCollection() }
        unmockkAll()
    }
}
