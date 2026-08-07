package com.neuroid.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SimulatedPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PUSH NOTIF", "PUSH RECEIVED")
        val message = intent.getStringExtra("message") ?: return
    }
}