package com.neuroid.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.NETWORK_STATE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDNetworkInfo
import com.neuroid.tracker.storage.NIDDataStoreManager
import java.util.Calendar

/**
 * requires android.permission.ACCESS_NETWORK_STATE, see manifest
 */
class NIDNetworkListener(private val connectivityManager: ConnectivityManager,
                         private val dataStoreManager: NIDDataStoreManager,
                         private val neuroID: NeuroID): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (ConnectivityManager.CONNECTIVITY_ACTION == it.action) {
              neuroID.setNIDNetworkInfo(onNetworkAction())
            }
        }
    }

    private fun onNetworkAction(): NIDNetworkInfo {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        var isWifi = false
        var isConnected = false
        activeNetworkInfo?.let {
            isWifi = activeNetworkInfo.type == TYPE_WIFI
            isConnected = activeNetworkInfo.isConnectedOrConnecting()
        }
        val networkEvent = NIDEventModel(
            ts = Calendar.getInstance().timeInMillis,
            type = NETWORK_STATE,
            isConnected = isConnected,
            isWifi = isWifi)
        dataStoreManager.saveEvent(networkEvent)
        return NIDNetworkInfo(isConnected, isWifi)
    }


}

