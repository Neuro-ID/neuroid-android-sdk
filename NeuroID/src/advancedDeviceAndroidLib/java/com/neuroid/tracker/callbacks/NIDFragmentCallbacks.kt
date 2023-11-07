package com.neuroid.tracker.callbacks

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONArray
import org.json.JSONObject

class NIDFragmentCallbacks(
    isChangeOrientation: Boolean
) : FragmentCallbacks(isChangeOrientation) {

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        NIDLog.d(msg="Fragment - Attached ${f::class.java.simpleName}")
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            if (NIDServiceTracker.screenName.isNullOrEmpty()) {
                NIDServiceTracker.screenName = "AppInit"
            }
            if (NIDServiceTracker.screenFragName.isNullOrEmpty()) {
                NIDServiceTracker.screenFragName = f::class.java.simpleName
            }
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()


            val metadataObj = JSONObject()
            metadataObj.put("component", "fragment")
            metadataObj.put("lifecycle", "attached")
            metadataObj.put("className", "${f::class.java.simpleName}")
            val attrJSON = JSONArray().put(metadataObj)

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
}