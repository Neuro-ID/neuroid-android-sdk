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
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CALL_IN_PROGRESS
import com.neuroid.tracker.events.CallInProgress
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.VersionChecker

class NIDCallActivityListener(
    private val neuroID: NeuroID,
    private val versionChecker: VersionChecker,
) : BroadcastReceiver() {
    private lateinit var intentFilter: IntentFilter
    lateinit var intent: Intent
    private val isReceiverRegistered = false
    private var callStateActive = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (context != null) {
            registerCustomTelephonyCallback(context)
        }
    }

    internal fun setCallActivityListener(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE,
            ) == PackageManager.PERMISSION_GRANTED && !isReceiverRegistered
        ) {
            NIDLog.d(msg = "Initializing call activity listener")
            intentFilter = IntentFilter("android.intent.action.PHONE_STATE")
            context.registerReceiver(this, intentFilter)
        } else {
            NIDLog.d(msg = "Permission to listen to call status not found")
            saveCallInProgressEvent(CallInProgress.UNAUTHORIZED.state)
        }
    }

    fun unregisterCallActivityListener(context: Context?) {
        if (isReceiverRegistered) {
            context?.unregisterReceiver(this)
        }
    }

    fun saveCallInProgressEvent(state: Int) {
        when (state) {
            CallInProgress.INACTIVE.state -> {
                callStateActive = false
                NIDLog.d(msg = "Call inactive")
                neuroID.captureEvent(
                    type = CALL_IN_PROGRESS,
                    cp = CallInProgress.INACTIVE.event,
                )
            }

            CallInProgress.ACTIVE.state -> {
                callStateActive = true
                NIDLog.d(msg = "Call in progress")
                neuroID.captureEvent(
                    type = CALL_IN_PROGRESS,
                    cp = CallInProgress.ACTIVE.event,
                )
            }

            CallInProgress.RINGING.state -> {
                NIDLog.d(msg = "Call Ringing")
                neuroID.captureEvent(
                    type = CALL_IN_PROGRESS,
                    cp = "$callStateActive",
                    attrs =
                        listOf(
                            mapOf(
                                "progress" to
                                    if (callStateActive) {
                                        "waiting"
                                    } else {
                                        "ringing"
                                    },
                            ),
                        ),
                )
            }

            CallInProgress.UNAUTHORIZED.state -> {
                NIDLog.d(msg = "Call status not authorized")
                neuroID.captureEvent(
                    type = CALL_IN_PROGRESS,
                    cp = CallInProgress.UNAUTHORIZED.event,
                )
            }
        }
    }

    private fun registerCustomTelephonyCallback(context: Context) {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (versionChecker.isBuildVersionGreaterThan31()) {
            NIDLog.d(msg = "SDK > 31")
            telephony.registerTelephonyCallback(
                context.mainExecutor,
                CustomTelephonyCallback { state ->
                    when (state) {
                        CallInProgress.INACTIVE.state -> {
                            saveCallInProgressEvent(CallInProgress.INACTIVE.state)
                        }

                        CallInProgress.RINGING.state -> {
                            saveCallInProgressEvent(CallInProgress.RINGING.state)
                        }

                        CallInProgress.ACTIVE.state -> {
                            saveCallInProgressEvent(CallInProgress.ACTIVE.state)
                        }
                    }
                },
            )
        } else {
            NIDLog.d(msg = "SDK < 31")
            telephony.listen(
                object : PhoneStateListener() {
                    override fun onCallStateChanged(
                        state: Int,
                        phoneNumber: String?,
                    ) {
                        when (state) {
                            TelephonyManager.CALL_STATE_IDLE -> {
                                saveCallInProgressEvent(CallInProgress.INACTIVE.state)
                            }

                            TelephonyManager.CALL_STATE_RINGING -> {
                                saveCallInProgressEvent(CallInProgress.RINGING.state)
                            }

                            // At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
                            TelephonyManager.CALL_STATE_OFFHOOK -> {
                                saveCallInProgressEvent(CallInProgress.ACTIVE.state)
                            }
                        }
                    }
                },
                PhoneStateListener.LISTEN_CALL_STATE,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
internal class CustomTelephonyCallback(callBack: CallBack) :
    TelephonyCallback(),
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
