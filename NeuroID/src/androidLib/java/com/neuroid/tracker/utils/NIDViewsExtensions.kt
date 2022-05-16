package com.neuroid.tracker.utils

import android.content.res.Resources
import android.view.View
import androidx.fragment.app.FragmentManager

fun View?.getIdOrTag(): String {

    return if (this == null) {
        "no_id"
    } else {
        if (this.id == View.NO_ID) {
            if(this.tag == null) {
                "no_id"
            } else {
                this.tag.toString()
            }
        } else {
            try {
                this.resources.getResourceEntryName(this.id)
            } catch (e: Resources.NotFoundException) {
                return "no_id"
            }
        }
    }
}

fun View.getParents(): String {
    return getParentsOfView(0, this)
}

private fun getParentsOfView(layers: Int, view: View): String {
    val childView = view.parent as View
    return if (layers == 3 || childView.id == android.R.id.content) "" else {
        "${childView.javaClass.simpleName}/${getParentsOfView(layers + 1, childView)}"
    }
}

fun FragmentManager.hasFragments(): Boolean {
    return this.fragments.any {
        val name = it::class.java.simpleName
        name != "NavHostFragment" || name != "SupportMapFragment"
    }
}