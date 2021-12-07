package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NeuroIdFragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks() {
    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        getDataStoreInstance(context)
            .saveEvent(NIDEventModel(type = WINDOW_LOAD, ts = System.currentTimeMillis()).getOwnJson())
        Log.d("NeuroId", "Fragment:${f::class.java.name} is onActivityCreated")
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
        // No operation
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        getDataStoreInstance(f.requireContext())
            .saveEvent(NIDEventModel(type = USER_INACTIVE, ts = System.currentTimeMillis()).getOwnJson())
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        getDataStoreInstance(f.requireContext())
            .saveEvent(NIDEventModel(type = WINDOW_UNLOAD, ts = System.currentTimeMillis()).getOwnJson())
        Log.d("NeuroId", "Fragment:${f::class.java.name} is onFragmentDetached")
    }
}