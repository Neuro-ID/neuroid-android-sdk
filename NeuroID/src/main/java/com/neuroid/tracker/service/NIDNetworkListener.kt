package com.neuroid.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.NETWORK_STATE
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * This class will listen for network change intent messages that are sent from the OS. In
 * NeuroID.init() we register this listener as a broadcast receiver which will filter intent
 * messages to pickup on any network activity. These messages contains the state of the network.
 * Listening to these messages requires android.permission.ACCESS_NETWORK_STATE set in the manifest.
 * Anytime we get network changes, we send these back as NETWORK_STATE events and update the
 * isConnected flag in NeuroID.
 *
 * In event of "not having a network connection" change, the listener will do the following action:
 * cancel any currently running pause or resume event collection jobs if event collection is not
 * running, it will take no action if event collection is running, it will wait 10 seconds and pause
 * event collection
 *
 * In event of "having a network connection" change, the listener will do the following action:
 * cancel any currently running pause or resume event collection jobs if event collection is
 * running, it will take no action if event collection is not running, it will wait 10 seconds and
 * resume event collection
 */
class NIDNetworkListener(
    private val connectivityManager: ConnectivityManager,
    private val neuroID: NeuroID,
    private val dispatcher: CoroutineContext,
    private val sleepIntervalResume: Long = SLEEP_INTERVAL_RESUME,
    private val sleepIntervalPause: Long = SLEEP_INTERVAL_PAUSE,
) : BroadcastReceiver() {
    companion object {
        const val SLEEP_INTERVAL_PAUSE = 10000L
        const val SLEEP_INTERVAL_RESUME = 2000L
    }

    private var haveNoNetworkJob: Job? = null
    private var haveNetworkJob: Job? = null

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        intent?.let {
            if (ConnectivityManager.CONNECTIVITY_ACTION == it.action) {
                neuroID.isConnected = onNetworkAction(context)

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
            haveNoNetworkJob =
                CoroutineScope(dispatcher).launch {
                    delay(sleepIntervalPause)
                    neuroID.sessionService.pauseCollection(false)
                }
        } else {
            if (!neuroID.isStopped() || neuroID.userID.isEmpty()) {
                return
            }
            haveNetworkJob =
                CoroutineScope(dispatcher).launch {
                    delay(sleepIntervalResume)
                    neuroID.sessionService.resumeCollection()
                }
        }
    }

    private fun onNetworkAction(context: Context?): Boolean {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        var isWifi = false
        var isConnected = false
        var networkConnectionType = neuroID.networkConnectionType
        activeNetworkInfo?.let {
            isWifi = activeNetworkInfo.type == TYPE_WIFI
            isConnected = activeNetworkInfo.isConnectedOrConnecting()
        }

        context?.let {
            networkConnectionType = neuroID.getNetworkType(it)
        }

        neuroID.networkConnectionType = networkConnectionType
        neuroID.captureEvent(
            type = NETWORK_STATE,
            isConnected = isConnected,
            isWifi = isWifi,
            ct = neuroID.networkConnectionType,
        )
        return isConnected
    }
}
