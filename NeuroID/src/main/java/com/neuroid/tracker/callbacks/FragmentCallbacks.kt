package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.events.registerTargetFromScreen
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDLogWrapper
import org.json.JSONArray
import org.json.JSONObject

abstract class FragmentCallbacks(isChangeOrientation: Boolean) : FragmentLifecycleCallbacks() {

    val blackListFragments = listOf("NavHostFragment", "SupportMapFragment")
    private var _isChangeOrientation = isChangeOrientation

    abstract override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context)

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        NIDLog.d(msg = "onFragmentViewCreated ${f::class.java.simpleName}")

    }

    override fun onFragmentViewCreated(
        fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
    ) {
        NIDLog.d(msg = "onFragmentViewCreated ${f::class.java.simpleName}")
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        super.onFragmentResumed(fm, f)
        NIDLog.d(
            msg = "Fragment - Resumed ${f.id} ${f.isVisible} ${f.tag} ${f::class.java.simpleName}"
        )

        // TODO skip on force start
        // On clients where we have trouble starting the registration do a force start
        if (NeuroID.getInstance()?.getForceStart() == true) {
            registerTargetFromScreen(
                f.requireActivity(),
                NIDLogWrapper(),
                getDataStoreInstance(),
                true,
                true,
                activityOrFragment = "fragment",
                parent = f::class.java.simpleName
            )
            return
        }

        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDLog.d(
                msg = "Fragment - Resumed - REGISTER TARGET ${f::class.java.simpleName}"
            )
            registerTargetFromScreen(
                f.requireActivity(),
                NIDLogWrapper(),
                getDataStoreInstance(),
                _isChangeOrientation.not(),
                true,
                activityOrFragment = "fragment",
                parent = f::class.java.simpleName
            )
            _isChangeOrientation = false
        } else {
            NIDLog.d(msg = "Fragment - Resumed - blacklisted ${f::class.java.simpleName}")
        }
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        super.onFragmentPaused(fm, f)
        NIDLog.d(msg = "Fragment - Paused ${f::class.java.simpleName}")
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        NIDLog.d(msg = "Fragment - Stopped ${f::class.java.simpleName}")
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        NIDLog.d(msg = "Fragment - Destroyed ${f::class.java.simpleName}")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        NIDLog.d(msg = "Fragment - Detached ${f::class.java.simpleName}")
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            val metadataObj = JSONObject()
            metadataObj.put("component", "fragment")
            metadataObj.put("lifecycle", "detached")
            metadataObj.put("className", "${f::class.java.simpleName}")
            val attrJSON = JSONArray().put(metadataObj)
            NIDLog.d(
                msg = "Fragment - Detached - WINDOW UNLOAD ${f::class.java.simpleName}"
            )
            getDataStoreInstance().saveEvent(
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