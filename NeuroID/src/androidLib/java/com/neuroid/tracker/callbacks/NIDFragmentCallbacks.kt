package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import java.util.*

class NIDFragmentCallbacks : FragmentManager.FragmentLifecycleCallbacks() {
    private val blackListFragments = listOf("NavHostFragment", "SupportMapFragment")
    private val stackFragments = Stack<String>()

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {

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
        // No Operation
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenName = "AppInit"
            NIDServiceTracker.screenFragName = f::class.java.simpleName
            val fragmentName = f::class.java.simpleName
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis()
                    )
                )
            if (stackFragments.isEmpty() || stackFragments.lastElement()
                    ?.contentEquals(fragmentName) == false
            ) {
                stackFragments.push(fragmentName)
                registerTargetFromScreen(f.requireActivity(), false)
            } else {
                registerTargetFromScreen(
                    f.requireActivity(),
                    changeOrientation = false,
                    registerListener = false
                )
            }
        }
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        // No Operation
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // No operation
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            stackFragments.pop()
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