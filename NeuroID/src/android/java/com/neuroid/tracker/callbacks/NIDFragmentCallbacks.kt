package com.neuroid.tracker.callbacks

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog

class NIDFragmentCallbacks(
    isChangeOrientation: Boolean
) : FragmentCallbacks(isChangeOrientation) {

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        NIDLog.d( msg="Fragment - Attached ${f::class.java.simpleName}")
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            if (NeuroID.screenName.isNullOrEmpty()) {
                NeuroID.screenName = "AppInit"
            }
            if (NeuroID.screenFragName.isNullOrEmpty()) {
                NeuroID.screenFragName = f::class.java.simpleName
            }
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        attrs = listOf(
                            mapOf(
                                "component" to "fragment",
                                "lifecycle" to "attached",
                                "className" to "${f::class.java.simpleName}"
                            )
                        )
                    )
                )
        }
    }
}