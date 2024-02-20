package com.neuroid.tracker.callbacks

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper

class NIDFragmentCallbacks : FragmentCallbacks(false) {
    var listFragment = arrayListOf<String>()

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        NIDLog.d(msg="onFragmentAttached ${f::class.java.simpleName}");

        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            if (NIDServiceTracker.screenName.isNullOrEmpty()) {
                NIDServiceTracker.screenName = "AppInit"
            }
            if (NIDServiceTracker.screenFragName.isNullOrEmpty()) {
                NIDServiceTracker.screenFragName = f::class.java.simpleName
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

            val concatName = f.toString().split(" ")
            val fragName = if (concatName.isNotEmpty()) {
                concatName[0]
            } else {
                ""
            }

            if (listFragment.contains(fragName)) {
                val index = listFragment.indexOf(fragName)
                if (index != listFragment.size - 1) {
                    listFragment.removeLast()
                    registerTargetFromScreen(
                        f.requireActivity(),
                        NIDLogWrapper(),
                        getDataStoreInstance(),
                        registerTarget = true,
                        registerListeners = false,
                        activityOrFragment = "fragment",
                        parent = f::class.java.simpleName
                    )
                }
            } else {
                listFragment.add(fragName)
                registerTargetFromScreen(
                    f.requireActivity(),
                    NIDLogWrapper(),
                    getDataStoreInstance(),
                    registerTarget = true,
                    registerListeners = true,
                    activityOrFragment = "fragment",
                    parent = f::class.java.simpleName
                )
            }
        }
    }

}