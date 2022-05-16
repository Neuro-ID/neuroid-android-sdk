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

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        NIDServiceTracker.screenName = "AppInit"
        NIDServiceTracker.screenFragName = f::class.java.simpleName

        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_LOAD,
                ts = System.currentTimeMillis()))

        registerViewsEventsForFragment(f.requireActivity())
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
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_FOCUS,
                ts = System.currentTimeMillis()))
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_BLUR,
                ts = System.currentTimeMillis()
            ))
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        getDataStoreInstance()
            .saveEvent(NIDEventModel(
                type = WINDOW_UNLOAD,
                ts = System.currentTimeMillis()
            ))
    }
}