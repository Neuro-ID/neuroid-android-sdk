package com.neuroid.tracker.utils

import android.view.View

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
            this.resources.getResourceEntryName(this.id)
        }
    }
}