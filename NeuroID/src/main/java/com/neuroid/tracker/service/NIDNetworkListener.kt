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
import kotlinx.coroutines.*
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

/**
 * This class will listen for network change intent messages that are sent from the OS.
 * In NeuroID.init() we register this listener as a broadcast receiver which will filter intent messages
 * to pickup on any network activity. These messages contains the state of the network.
 * Listening to these messages requires android.permission.ACCESS_NETWORK_STATE set in the
 * manifest. Anytime we get network changes, we send these back as NETWORK_STATE events and
 * update the isConnected flag in NeuroID.
 *
 * In event of "not having a network connection" change, the listener will do the following action:
 * cancel any currently running pause or resume event collection jobs
 * if event collection is not running, it will take no action
 * if event collection is running, it will wait 10 seconds and pause event collection
 *
 * In event of "having a network connection" change, the listener will do the following action:
 * cancel any currently running pause or resume event collection jobs
 * if event collection is running, it will take no action
 * if event collection is not running, it will wait 10 seconds and resume event collection
 */
class NIDNetworkListener(private val connectivityManager: ConnectivityManager,
                         private val dataStoreManager: NIDDataStoreManager,
                         private val neuroID: NeuroID,
                         private val dispatcher: CoroutineContext,
                         private val sleepInterval: Long = SLEEP_INTERVAL): BroadcastReceiver() {

    companion object {
        const val SLEEP_INTERVAL = 10000L
    }

    private var haveNoNetworkJob: Job? = null
    private var haveNetworkJob: Job? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (ConnectivityManager.CONNECTIVITY_ACTION == it.action) {
                neuroID.isConnected = onNetworkAction()

                // act on the network change
                handleNetworkChange()
            }
        }
    }

    private fun cancelJobs() {
        haveNetworkJob?.cancel()
        haveNoNetworkJob?.cancel()
    }

    private fun handleNetworkChange() {
        cancelJobs()
        if (!neuroID.isConnected) {
            if (neuroID.isStopped()) {
                return
            }
            haveNoNetworkJob = CoroutineScope(dispatcher).launch {
                delay(sleepInterval)
                neuroID.pauseCollection(false)
            }
        } else {
            if (!neuroID.isStopped()) {
                return
            }
            haveNetworkJob = CoroutineScope(dispatcher).launch {
                delay(sleepInterval)
                neuroID.resumeCollection()
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
