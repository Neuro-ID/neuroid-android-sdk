package com.neuroid.tracker.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.forEach
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
    return getAllSubViews(viewGroup).firstOrNull()
}

private fun getAllSubViews(viewGroup: ViewGroup): List<ReactRootView> {
    val list = ArrayList<ReactRootView>()

    val view: ReactRootView? = viewGroup.children.firstOrNull { it is ReactRootView} as? ReactRootView

    if (view == null) {
        viewGroup.forEach {
            if (it is ViewGroup) {
                list.addAll(getAllSubViews(it))
            }
        }
    } else {
        list.add(view)
    }

    return list.toList()
}