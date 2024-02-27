package com.neuroid.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.NETWORK_STATE
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import java.util.Calendar

/**
 * This class will listen for network change intent messages that are sent from the OS.
 * In NeuroID.init() we register this listener as a broadcast receiver which will filter intent messages
 * to pickup on any network activity. These messages contains the state of the network.
 * Listening to these messages requires android.permission.ACCESS_NETWORK_STATE set in the
 * manifest. Anytime we get network changes, we send these back as NETWORK_STATE events and
 * update the isConnected flag in NeuroID.
 */
class NIDNetworkListener(private val connectivityManager: ConnectivityManager,
                         private val dataStoreManager: NIDDataStoreManager,
                         private val neuroID: NeuroID): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (ConnectivityManager.CONNECTIVITY_ACTION == it.action) {
                neuroID.isConnected = onNetworkAction()
            }
        }
    }

    private fun onNetworkAction(): Boolean {
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
        return isConnected
    }


}

