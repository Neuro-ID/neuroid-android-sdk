package com.neuroid.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Simulates a headless FCM-style "data push" wake-up without any real Firebase
 * infrastructure. Because this receiver is declared in the manifest, Android
 * will start this app's process (running [ApplicationMain.onCreate] first,
 * exactly like a real FCM push would) even if the app was previously killed,
 * deliver this broadcast, and then let the process die again once idle -
 * no Activity/UI is ever shown and no user input is involved.
 *
 * Trigger it from the command line (app does not need to be running):
 *
 * ```
 * adb shell am broadcast \
 *   -a com.neuroid.example.SIMULATE_PUSH \
 *   -n com.neuroid.example.debug/com.neuroid.example.SimulatedPushReceiver \
 *   --es message "Hello from simulated push"
 * ```
 */
class SimulatedPushReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SIMULATE_PUSH = "com.neuroid.example.SIMULATE_PUSH"
        const val EXTRA_MESSAGE = "message"
        private const val TAG = "SimulatedPushReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SIMULATE_PUSH) {
            return
        }

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Simulated push message"
        Log.i(TAG, "Headless push received: $message")

        NotificationHelper.showMessage(context, message)
    }
}

