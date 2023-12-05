package com.sample.neuroid.us.activities

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
import androidx.core.content.ContextCompat
import com.neuroid.tracker.utils.NIDLog


class NIDCallActivityListener : BroadcastReceiver() {
    lateinit var intentFilter: IntentFilter
    lateinit var receiverNIDCallActivityListener: NIDCallActivityListener
    lateinit var intent: Intent
    init {
        NIDLog.d("NeuroID call activity", "initializing receiver")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context?, intent: Intent?) {
        NIDLog.d( msg="ON RECEIVE!")
        if (context != null) {
            NIDLog.d(msg="listening to broadcast receiver")
            registerCustomTelephonyCallback(context)
        }
    }
}


fun registerCustomTelephonyCallback(context: Context) {
    val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        NIDLog.d(msg="SDK > 31")
        telephony.registerTelephonyCallback(
            context.mainExecutor, CustomTelephonyCallback(object : CallBack {
                override fun callStateChanged(state: Int) {
                    NIDLog.d(msg= "CURRENT CALL STATE: $state")
                }
            })
        )
    } else {
        NIDLog.d(msg="SDK < 31")
        telephony.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                NIDLog.d(msg= "CURRENT CALL STATE: $state")
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        NIDLog.d(msg="No call")
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        NIDLog.d(msg="Incoming call")
                        // Incoming call
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        NIDLog.d(msg="Call in progress")

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

