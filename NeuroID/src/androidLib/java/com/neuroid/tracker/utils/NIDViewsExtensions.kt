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

fun View.getRandomId(): String {
    val viewCoordinates = "${this.x}_${this.y}".replace(".","")

    return "${this.javaClass.simpleName}_$viewCoordinates"
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