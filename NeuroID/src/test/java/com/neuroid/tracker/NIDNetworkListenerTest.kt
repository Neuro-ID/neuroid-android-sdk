package com.neuroid.tracker

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.NetworkInfo
import com.neuroid.tracker.events.NETWORK_STATE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDNetworkListener
import com.neuroid.tracker.storage.NIDDataStoreManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Test
import java.util.Calendar

class NIDNetworkListenerTest {

    @Test
    fun test_networkListener_wifi_connected() {
        networkListenerTestHarness(TYPE_WIFI, true, true, true)
    }

    @Test
    fun test_networkListener_mobile_connected() {
        networkListenerTestHarness(TYPE_MOBILE, true, true, false)
    }

    @Test
    fun test_networkListener_mobile_not_connected() {
        networkListenerTestHarness(TYPE_MOBILE, false, false, false)
    }

    @Test
    fun test_networkListener_wifi_not_connected() {
        networkListenerTestHarness(TYPE_WIFI, false, false, true)
    }

    fun networkListenerTestHarness(connectionType: Int,
                                   isConnectedOrConnecting: Boolean,
                                   isConnectedAssert: Boolean,
                                   isWifiAssert: Boolean) {
        val calendar = mockk<Calendar>()
        every {calendar.timeInMillis} returns 5
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        val neuroID = mockk<NeuroID>()
        every {neuroID.isConnected = any()} just runs
        val networkInfo = mockk<NetworkInfo>()
        every { networkInfo.type } returns connectionType
        every { networkInfo.isConnectedOrConnecting } returns isConnectedOrConnecting
        val connectivityManager = mockk<ConnectivityManager>()
        every { connectivityManager.activeNetworkInfo } returns networkInfo
        val dataStoreManager = mockk<NIDDataStoreManager>()
        every { dataStoreManager.saveEvent(any()) } just runs
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { intent.action } returns ConnectivityManager.CONNECTIVITY_ACTION
        val listener = NIDNetworkListener(connectivityManager, dataStoreManager, neuroID)
        listener.onReceive(context, intent)

        val networkEvent = NIDEventModel(
            ts = Calendar.getInstance().timeInMillis,
            type = NETWORK_STATE,
            isConnected = isConnectedAssert,
            isWifi = isWifiAssert)

        verify { dataStoreManager.saveEvent(networkEvent) }
        verify { neuroID.isConnected = isConnectedAssert}
        unmockkAll()
    }
}