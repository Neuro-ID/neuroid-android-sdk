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
import com.neuroid.tracker.storage.getDataStoreInstance

/**
 * Need to use the deprecated NetworkInfo to be compliant with API21,
 * NetworkCapabilities is available in >= API23
 *
 * requires android.permission.ACCESS_NETWORK_STATE, see manifest
 */
class NIDNetworkListener: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { safeIntent ->
            context?.let {safeContext ->
                if (ConnectivityManager.CONNECTIVITY_ACTION == safeIntent.action) {
                   NeuroID.getInstance()?.setNIDNetworkInfo(isConnected(safeContext))
                }
            }
        }
    }

    private fun isConnected(context: Context): NIDNetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = cm.getActiveNetworkInfo()
        var isWifi = false
        var isConnected = false
        activeNetworkInfo?.let {
            isWifi =   activeNetworkInfo.type == TYPE_WIFI
            isConnected = activeNetworkInfo.isConnectedOrConnecting()
        }
        val networkEvent = NIDEventModel(ts = System.currentTimeMillis(),
            type= NETWORK_STATE,
            isConnected = isConnected,
            isWifi = isWifi)
        getDataStoreInstance().saveEvent(networkEvent)
        return NIDNetworkInfo(isConnected, isWifi)
    }


}

