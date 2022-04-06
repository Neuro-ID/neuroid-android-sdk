package com.neuroid.tracker.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import com.facebook.react.ReactRootView

fun View?.getIdOrTag(): String {

    return if (this == null) {
        "no_id"
    } else {
        return if (this.tag == null) {
            if(this.contentDescription == null) {
                this.id.toString()
            } else {
                this.contentDescription.toString()
            }
        } else {
            this.tag.toString()
        }
    }
}

fun getReactRoot(viewGroup: ViewGroup): ReactRootView? {
    val listChildren: List<View> = viewGroup.children.filter { it is ViewGroup}.toList()
    val rootReact = listChildren.firstOrNull { it is ReactRootView }

    return when {
        listChildren.isEmpty() -> null
        rootReact != null -> {
            rootReact as ReactRootView
        }
        else -> {
            listChildren.map {
                getReactRoot(it as ViewGroup)
            }.firstOrNull { it is ReactRootView }
        }
    }
}

fun FragmentManager.hasFragments(): Boolean {
    return this.fragments.any {
        val name = it::class.java.simpleName
        name != "NavHostFragment" || name != "SupportMapFragment"
    }
}