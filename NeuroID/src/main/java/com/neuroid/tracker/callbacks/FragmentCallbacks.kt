package com.neuroid.tracker.callbacks

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.RegistrationIdentificationHelper
import com.neuroid.tracker.events.WINDOW_LOAD
import com.neuroid.tracker.events.WINDOW_UNLOAD
import com.neuroid.tracker.utils.NIDLogWrapper

class FragmentCallbacks(
    isChangeOrientation: Boolean,
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
    val registrationHelper: RegistrationIdentificationHelper,
) : FragmentLifecycleCallbacks() {
    private var _isChangeOrientation = isChangeOrientation

    var listFragment = arrayListOf<String>()
    val blackListFragments = listOf("NavHostFragment", "SupportMapFragment")

    override fun onFragmentAttached(
        fm: FragmentManager,
        f: Fragment,
        context: Context,
    ) {
        val className = f::class.java.simpleName
        logger.d(msg = "onFragmentAttached $className")

        if (blackListFragments.any { it == className }.not()) {
            if (NeuroID.screenName.isEmpty()) {
                NeuroID.screenName = "AppInit"
            }
            if (NeuroID.screenFragName.isEmpty()) {
                NeuroID.screenFragName = className
            }

            neuroID.captureEvent(
                type = WINDOW_LOAD,
                attrs =
                    listOf(
                        mapOf(
                            "component" to "fragment",
                            "lifecycle" to "attached",
                            "className" to className,
                        ),
                    ),
            )

            val concatName = f.toString().split(" ")
            val fragName =
                if (concatName.isNotEmpty()) {
                    concatName[0]
                } else {
                    ""
                }

            if (listFragment.contains(fragName)) {
                val index = listFragment.indexOf(fragName)
                if (index != listFragment.size - 1) {
                    listFragment.removeLast()
                    registrationHelper.registerTargetFromScreen(
                        f.requireActivity(),
                        registerTarget = true,
                        registerListeners = false,
                        activityOrFragment = "fragment",
                        parent = className,
                    )
                }
            } else {
                listFragment.add(fragName)
                registrationHelper.registerTargetFromScreen(
                    f.requireActivity(),
                    registerTarget = true,
                    registerListeners = true,
                    activityOrFragment = "fragment",
                    parent = className,
                )
            }
        }
    }

    override fun onFragmentCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?,
    ) {
        logger.d(msg = "onFragmentViewCreated ${f::class.java.simpleName}")
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        logger.d(msg = "onFragmentViewCreated ${f::class.java.simpleName}")
    }

    override fun onFragmentResumed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentResumed(fm, f)
        val simpleClassName = f::class.java.simpleName

        logger.d(
            msg = "Fragment - Resumed ${f.id} ${f.isVisible} ${f.tag} $simpleClassName",
        )

        // TODO skip on force start
        // On clients where we have trouble starting the registration do a force start
        if (neuroID.shouldForceStart()) {
            registrationHelper.registerTargetFromScreen(
                f.requireActivity(),
                true,
                true,
                activityOrFragment = "fragment",
                parent = simpleClassName,
            )
            return
        }

        if (blackListFragments.any { it == simpleClassName }.not()) {
            logger.d(
                msg = "Fragment - Resumed - REGISTER TARGET $simpleClassName",
            )
            registrationHelper.registerTargetFromScreen(
                f.requireActivity(),
                _isChangeOrientation.not(),
                true,
                activityOrFragment = "fragment",
                parent = simpleClassName,
            )
            _isChangeOrientation = false
        } else {
            logger.d(msg = "Fragment - Resumed - blacklisted $simpleClassName")
        }
    }

    override fun onFragmentPaused(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentPaused(fm, f)
        logger.d(msg = "onFragmentPaused ${f::class.java.simpleName}")
    }

    override fun onFragmentStopped(
        fm: FragmentManager,
        f: Fragment,
    ) {
        logger.d(msg = "onFragmentStopped ${f::class.java.simpleName}")
    }

    override fun onFragmentDestroyed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        logger.d(msg = "onFragmentDestroyed ${f::class.java.simpleName}")
    }

    override fun onFragmentDetached(
        fm: FragmentManager,
        f: Fragment,
    ) {
        val className = f::class.java.simpleName
        logger.d(msg = "Fragment - Detached $className")
        if (blackListFragments.any { it == className }.not()) {
            logger.d(
                msg = "Fragment - Detached - WINDOW UNLOAD $className",
            )

            neuroID.captureEvent(
                type = WINDOW_UNLOAD,
                attrs =
                    listOf(
                        mapOf(
                            "component" to "fragment",
                            "lifecycle" to "detached",
                            "className" to className,
                        ),
                    ),
            )
        }
    }
}
