package com.neuroid.tracker.service

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
import com.neuroid.tracker.events.CallInProgress
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.VersionChecker
import java.util.Calendar

class NIDCallActivityListener(
    private val dataStoreManager: NIDDataStoreManager,
    private val versionChecker: VersionChecker
) : BroadcastReceiver() {
    private lateinit var intentFilter: IntentFilter
    lateinit var intent: Intent
    private val isReceiverRegistered = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            registerCustomTelephonyCallback(context)
        }
    }

    internal fun setCallActivityListener(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED && !isReceiverRegistered
        ) {
            NIDLog.d(msg = "Initializing call activity listener")
            intentFilter = IntentFilter("android.intent.action.PHONE_STATE")
            context.registerReceiver(this, intentFilter)
        } else {
            NIDLog.d(msg = "Permission to listen to call status not found")
            saveCallInProgressEvent(9)
        }
    }

    fun unregisterCallActivityListener(context: Context?) {
        if (isReceiverRegistered) {
            context?.unregisterReceiver(this)
        }
    }

    fun saveCallInProgressEvent(state: Int) {
        when (state) {
            0 -> {
                NIDLog.d(msg = "Call inactive")
                dataStoreManager.saveEvent(
                    NIDEventModel(
                        type = CALL_IN_PROGRESS,
                        cp = CallInProgress.INACTIVE.state,
                        ts = Calendar.getInstance().timeInMillis,
                    )
                )
            }

            2 -> {
                NIDLog.d(msg = "Call in progress")
                dataStoreManager.saveEvent(
                    NIDEventModel(
                        type = CALL_IN_PROGRESS,
                        cp = CallInProgress.ACTIVE.state,
                        ts = Calendar.getInstance().timeInMillis,
                    )
                )

            }

            9 -> {
                NIDLog.d(msg = "Call status not authorized")
                dataStoreManager.saveEvent(
                    NIDEventModel(
                        type = CALL_IN_PROGRESS,
                        cp = CallInProgress.UNAUTHORIZED.state,
                        ts = Calendar.getInstance().timeInMillis,
                    )
                )
            }
        }
    }

    private fun registerCustomTelephonyCallback(context: Context) {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (versionChecker.isBuildVersionGreaterThan31()) {
            NIDLog.d(msg = "SDK > 31")
            telephony.registerTelephonyCallback(
                context.mainExecutor, CustomTelephonyCallback { state ->
                    when (state) {
                        0 -> {
                            saveCallInProgressEvent(0)
                        }

                        2 -> {
                            saveCallInProgressEvent(2)
                        }
                    }
                }
            )
        } else {
            NIDLog.d(msg = "SDK < 31")
            telephony.listen(object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_IDLE -> {
                            saveCallInProgressEvent(0)
                        }

                        // At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            saveCallInProgressEvent(2)

                        }
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE)
        }
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

internal fun interface CallBack {
    fun callStateChanged(state: Int)
}

