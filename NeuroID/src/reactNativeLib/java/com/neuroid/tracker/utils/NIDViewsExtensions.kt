package com.neuroid.tracker.utils

import android.view.View

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

fun View.getParents(): String {
    return getParentsOfView(0, this)
}

private fun getParentsOfView(layers: Int, view: View): String {
    val childView = view.parent as View
    return if (layers == 3 || childView.id == android.R.id.content) "" else {
        "${childView.javaClass.simpleName}/${getParentsOfView(layers + 1, childView)}"
    }
}