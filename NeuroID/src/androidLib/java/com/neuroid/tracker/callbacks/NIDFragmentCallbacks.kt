package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.events.registerTargetFromScreen
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONArray
import org.json.JSONObject

class NIDFragmentCallbacks(
    isChangeOrientation: Boolean
) : FragmentManager.FragmentLifecycleCallbacks() {
    private val blackListFragments = listOf("NavHostFragment", "SupportMapFragment")
    private var _isChangeOrientation = isChangeOrientation


    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
//        NIDLog.d("NID-Activity", "Fragment - Attached ${f::class.java.simpleName}")
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenName = "AppInit"
            NIDServiceTracker.screenFragName = f::class.java.simpleName
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()


            val metadataObj = JSONObject()
            metadataObj.put("component", "fragment")
            metadataObj.put("lifecycle", "attached")
            metadataObj.put("className", "${f::class.java.simpleName}")
            val attrJSON = JSONArray().put(metadataObj)

//            NIDLog.d(
//                "NID-Activity",
//                "Fragment - Attached - WINDOW LOAD ${f::class.java.simpleName}"
//            )
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        attrs = attrJSON
                    )
                )
        }
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        // No Operation
//        NIDLog.d("NID-Activity", "Fragment - Created ${f::class.java.simpleName}")
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
//        NIDLog.d("NID-Activity", "Fragment - View Created ${f.id}")
        NIDLog.d("Neuro ID", "NIDDebug onFragmentViewCreated ${f::class.java.simpleName}");
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        super.onFragmentResumed(fm, f)
        NIDLog.d("Neuro ID", "NIDDebug onFragmentResumed${f::class.java.simpleName}");
//        NIDLog.d(
//            "NID-Activity",
//            "Fragment - Resumed ${f.id} ${f.isVisible} ${f.tag} ${f::class.java.simpleName}"
//        )

        // TODO skip on force start
        // On clients where we have trouble starting the registration do a force start
        if (NeuroID.getInstance()?.getForceStart() == true) {
            registerTargetFromScreen(
                f.requireActivity(),
                true,
                activityOrFragment = "fragment",
                parent = f::class.java.simpleName
            )
            return
        }

        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
//            NIDLog.d(
//                "NID-Activity",
//                "Fragment - Resumed - REGISTER TARGET ${f::class.java.simpleName}"
//            )
            registerTargetFromScreen(
                f.requireActivity(),
                _isChangeOrientation.not(),
                activityOrFragment = "fragment",
                parent = f::class.java.simpleName
            )
            _isChangeOrientation = false
        } else {
//            NIDLog.d("NID-Activity", "Fragment - Resumed - blacklisted ${f::class.java.simpleName}")
        }
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        super.onFragmentPaused(fm, f)
//        NIDLog.d("NID-Activity", "Fragment - Paused ${f::class.java.simpleName}")
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
//        NIDLog.d("NID-Activity", "Fragment - Stopped ${f::class.java.simpleName}")
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        //No operation
//        NIDLog.d("NID-Activity", "Fragment - Destroyed ${f::class.java.simpleName}")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
//        NIDLog.d("NID-Activity", "Fragment - Detached ${f::class.java.simpleName}")
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            val metadataObj = JSONObject()
            metadataObj.put("component", "fragment")
            metadataObj.put("lifecycle", "detached")
            metadataObj.put("className", "${f::class.java.simpleName}")
            val attrJSON = JSONArray().put(metadataObj)

//            NIDLog.d(
//                "NID-Activity",
//                "Fragment - Detached - WINDOW UNLOAD ${f::class.java.simpleName}"
//            )
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