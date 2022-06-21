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
    private var listFragment = arrayListOf<String>()

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        if (blackListFragments.any { it == f::class.java.simpleName }.not()) {
            NIDServiceTracker.screenFragName = f::class.java.simpleName

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_LOAD,
                        ts = System.currentTimeMillis()
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
                if (index != listFragment.size -1) {
                    listFragment.removeLast()
                    registerTargetFromScreen(f.requireActivity(),
                        registerTarget = true,
                        registerListeners = false
                    )
                }
            } else {
                listFragment.add(fragName)
                registerTargetFromScreen(f.requireActivity(),
                    registerTarget = true,
                    registerListeners = true
                )
            }
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