package com.neuroid.tracker.utils

import android.content.res.Resources
import android.view.View
import androidx.fragment.app.FragmentManager

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


fun View.getParentsOfView(layers: Int, view: View, log: NIDLogWrapper): String {
    return if (view.parent is View) {
        val childView = view.parent as View
        if (layers == 3 || childView.id == android.R.id.content) "" else {
            "${childView.javaClass.simpleName}/${getParentsOfView(layers + 1, childView, log)}"
        }
    } else {
        log.e("Neuro ID", "instance ${view.parent?.javaClass?.name} is not a view!")
        "not_a_view"
    }
}

fun FragmentManager.hasFragments(): Boolean {
    return this.fragments.any {
        val name = it::class.java.simpleName
        name != "NavHostFragment" || name != "SupportMapFragment"
    }
}