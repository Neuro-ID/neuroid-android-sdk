package com.neuroid.tracker.callbacks

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog

class NIDCallActivityListener : BroadcastReceiver() {
    lateinit var intentFilter: IntentFilter
    lateinit var intent: Intent

    internal fun setCallActivityListener(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NIDLog.d(msg = "Initializing call activity listener")
            intentFilter = IntentFilter("android.intent.action.PHONE_STATE")
            context.registerReceiver(this, intentFilter)
        } else {
            NIDLog.d(msg = "Permission to listen to call status not found")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            registerCustomTelephonyCallback(context)
        }
    }

    fun unregisterCallActivityListener(context: Context?) {
        context?.unregisterReceiver(this)
    }
}

fun registerCustomTelephonyCallback(context: Context) {
    val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        NIDLog.d(msg = "SDK > 31")
        telephony.registerTelephonyCallback(
            context.mainExecutor, CustomTelephonyCallback(object : CallBack {
                override fun callStateChanged(state: Int) {
                    NIDLog.d(msg = "CURRENT CALL STATE: $state")
                    when (state) {
                    // No activity
                        0 -> {
                            getDataStoreInstance().saveEvent(
                                NIDEventModel(
                                    type = CALL_IN_PROGRESS,
                                    cp = false,
                                    ts = System.currentTimeMillis()
                                )
                            )
                        }
                    //  At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
                        2 -> {
                            getDataStoreInstance().saveEvent(
                                NIDEventModel(
                                    type = CALL_IN_PROGRESS,
                                    cp = true,
                                    ts = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            })
        )
    } else {
        NIDLog.d(msg = "SDK < 31")
        telephony.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        getDataStoreInstance().saveEvent(
                            NIDEventModel(
                                type = CALL_IN_PROGRESS, cp = false, ts = System.currentTimeMillis()
                            )
                        )
                    }

                    // At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        NIDLog.d(msg = "Call in progress")
                        getDataStoreInstance().saveEvent(
                            NIDEventModel(
                                type = CALL_IN_PROGRESS, cp = true, ts = System.currentTimeMillis()
                            )
                        )

                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
internal class CustomTelephonyCallback(callBack: CallBack) : TelephonyCallback(),
    TelephonyCallback.CallStateListener {
    private val mCallBack: CallBack

    init {
        mCallBack = callBack
    }

    override fun onCallStateChanged(state: Int) {
        mCallBack.callStateChanged(state)
    }
}

internal interface CallBack {
    fun callStateChanged(state: Int)
}

