package com.neuroid.tracker.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.neuroid.tracker.utils.NIDLogWrapper

fun View?.getIdOrTag(): String {
    return if (this == null) {
        "no_id"
    } else {
        if (!this.contentDescription.isNullOrEmpty()) {
            this.contentDescription.toString()
        } else {
            if (this.id == View.NO_ID) {
                if (this.tag == null) {
                    this.getRandomId()
                } else {
                    this.tag.toString()
                }
            } else {
                try {
                    this.resources.getResourceEntryName(this.id)
                } catch (e: Resources.NotFoundException) {
                    return this.getRandomId()
                }
            }
        }
    }
}

fun View.getRandomId(): String {
    val viewCoordinates = "${this.x}_${this.y}".replace(".", "")

    return "${this.javaClass.simpleName}_$viewCoordinates"
}

fun View.getParents(logger: NIDLogWrapper): String {
    return getParentsOfView(0, this, logger)
}

fun View.getParentsOfView(
    layers: Int,
    view: View,
    log: NIDLogWrapper,
): String {
    return if (view.parent is View) {
        val childView = view.parent as View
        if (layers == 3 || childView.id == android.R.id.content) {
            ""
        } else {
            "${childView.javaClass.simpleName}/${getParentsOfView(layers + 1, childView, log)}"
        }
    } else {
        log.e(msg = "instance ${view.parent?.javaClass?.name} is not a view!")
        "not_a_view"
    }
}

fun View.getParentActivity(): String? {
    var context: Context? = this.context
    while (context is Context) {
        if (context is Activity) {
            return context::class.java.name
        }
        if (context is ContextWrapper) {
            context = context.baseContext
        } else {
            return null
        }
    }
    return null
}

fun View.getParentFragment(): String? {
    var context = this.context
    while (context is ContextWrapper) {
        if (context is FragmentActivity) {
            val fragments = (context as FragmentActivity).supportFragmentManager.fragments
            for (fragment in fragments) {
                val foundFragment = findFragment(this, fragment)
                if (foundFragment != null) {
                    val result = buildFragAncestry(foundFragment).reversed().joinToString(separator = "/")
                    return "$result/"
                }
            }
        }
        context = context.baseContext
    }
    return null
}

private fun findFragment(
    view: View,
    fragment: Fragment?,
): Fragment? {
    if (fragment == null || fragment.view == null) {
        return null
    }
    if (view == fragment.view || view.parent == fragment.view) {
        return fragment
    }

    val childFragments = fragment.childFragmentManager.fragments
    for (childFragment in childFragments) {
        val foundFragment = findFragment(view, childFragment)
        if (foundFragment != null) {
            return foundFragment
        }
    }
    return null
}

private fun buildFragAncestry(fragment: Fragment): List<String> {
    return mutableListOf<String>(
        fragment::class.java.name,
    ) +
        if (fragment.parentFragment != null) {
            buildFragAncestry(fragment.requireParentFragment())
        } else {
            mutableListOf<String>()
        }
}
