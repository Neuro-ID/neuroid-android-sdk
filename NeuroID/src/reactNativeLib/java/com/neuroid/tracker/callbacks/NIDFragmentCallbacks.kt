package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONObject
import org.json.JSONArray


class NIDFragmentCallbacks : FragmentManager.FragmentLifecycleCallbacks() {
    private val blackListFragments = listOf("NavHostFragment", "SupportMapFragment")
    private var listFragment = arrayListOf<String>()

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        NIDLog.d("Neuro ID", "NIDDebug onFragmentAttached ${f::class.java.simpleName}");

        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenFragName = f::class.java.simpleName
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            val jsonObject = JSONObject()
            jsonObject.put("component", "fragment")
            jsonObject.put("lifecycle", "attached")
            jsonObject.put("className", "${f::class.java.simpleName}")

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        metadata = jsonObject
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
                    registerTarget = true,
                    registerListeners = true,
                    activityOrFragment = "fragment",
                    parent = f::class.java.simpleName
                )
            }
        }
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        // No Operation
        NIDLog.d("Neuro ID", "NIDDebug onFragmentViewCreated ${f::class.java.simpleName}");

    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        //No operation
        NIDLog.d("Neuro ID", "NIDDebug onFragmentCreated");

    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        //No operation
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            val metadataObj = JSONObject()
            metadataObj.put("component", "fragment")
            metadataObj.put("lifecycle", "detached")
            metadataObj.put("className", "${f::class.java.simpleName}")
            val attrJSON = JSONArray().put(metadataObj)

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_UNLOAD,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        attrs = attrJSON
                    )
                )
        }
    }
}