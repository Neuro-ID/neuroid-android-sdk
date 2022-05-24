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

class NIDFragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks() {
    private val blackListFragments = listOf("NavHostFragment","SupportMapFragment")

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenName = "AppInit"
            NIDServiceTracker.screenFragName = f::class.java.simpleName

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis()
                    )
                )

            registerTargetFromScreen(f.requireActivity(), false)
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
        //No operation
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
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_UNLOAD,
                        ts = System.currentTimeMillis()
                    )
                )
        }
    }
}