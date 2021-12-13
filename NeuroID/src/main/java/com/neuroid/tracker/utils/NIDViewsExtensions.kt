package com.neuroid.tracker.utils

import android.view.View

fun View.getIdOrTag(): String{
    val idName = if (this.id == View.NO_ID) {
        "no_id"
    } else {
        this.resources.getResourceEntryName(this.id)
    }

    return if (idName == "no_id") {
        if(this.tag == null) {
            "no_id"
        } else {
            this.tag.toString()
        }
    } else {
        idName
    }
}