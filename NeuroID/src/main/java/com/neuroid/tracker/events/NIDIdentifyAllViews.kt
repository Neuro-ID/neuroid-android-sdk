package com.neuroid.tracker.events

import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.core.view.forEach
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

fun identifyAllViews(viewParent: ViewGroup) {
    viewParent.forEach {
        if (it is Spinner) {
            it.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SELECT_CHANGE,
                                ts = System.currentTimeMillis()
                            ).getOwnJson())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //No Op
                }
            }
        } else {
            if (it is ViewGroup) {
                identifyAllViews(it)
            }
        }
    }
}