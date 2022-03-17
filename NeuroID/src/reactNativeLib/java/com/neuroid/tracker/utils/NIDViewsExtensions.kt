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