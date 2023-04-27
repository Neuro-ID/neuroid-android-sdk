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

class NIDFragmentCallbacks(
    isChangeOrientation: Boolean
)
    : FragmentManager.FragmentLifecycleCallbacks() {
    private val blackListFragments = listOf("NavHostFragment","SupportMapFragment")
    private var _isChangeOrientation = isChangeOrientation



    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {

        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenName = "AppInit"
            NIDServiceTracker.screenFragName = f::class.java.simpleName
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_LOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData
                ))
        }
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        // No Operation
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        NIDLog.d("Neuro ID", "NIDDebug onFragmentViewCreated ${f::class.java.simpleName}");

        // On clients where we have trouble starting the registration do a force start
        if (NeuroID.getInstance()?.getForceStart() == true){
            registerTargetFromScreen(f.requireActivity(), true)
            return
        }
        
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            registerTargetFromScreen(f.requireActivity(), _isChangeOrientation.not())
            _isChangeOrientation = false
        } else {
            NIDLog.d("Neuro ID", "NIDDebug Fragment blacklisted ${f::class.java.simpleName}");
        }
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation

    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        //No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_UNLOAD,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData
                ))
        }
    }
}