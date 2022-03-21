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
    private val nameListExclude = listOf(
        "androidx.navigation.fragment.NavHostFragment",
        "com.google.android.gms.maps.SupportMapFragment"
    )

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        val isExcludeName = nameListExclude.any { it ==  f::class.java.name }

        if (!isExcludeName) {
            NIDServiceTracker.screenFragName = f::class.java.simpleName

            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_LOAD,
                    et = "FRAGMENT",
                    ts = System.currentTimeMillis()))
        }
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        // No Operation
    }

    override fun onFragmentActivityCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?
    ) {
        // No operation
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        val isExcludeName = nameListExclude.any { it ==  f::class.java.name }

        if (!isExcludeName) {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_FOCUS,
                    et = "FRAGMENT",
                    ts = System.currentTimeMillis()))
        }
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        val isExcludeName = nameListExclude.any { it ==  f::class.java.name }
        if (!isExcludeName) {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_BLUR,
                    et = "FRAGMENT",
                    ts = System.currentTimeMillis()
                ))
        }
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        val isExcludeName = nameListExclude.any { it ==  f::class.java.name }

        if (!isExcludeName) {
            getDataStoreInstance()
                .saveEvent(NIDEventModel(
                    type = WINDOW_UNLOAD,
                    et = "FRAGMENT",
                    ts = System.currentTimeMillis()
                ))
        }
    }
}