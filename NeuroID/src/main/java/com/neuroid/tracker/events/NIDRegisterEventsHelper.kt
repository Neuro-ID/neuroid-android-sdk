package com.neuroid.tracker.events

import android.app.Activity
import android.view.View
import android.widget.EditText
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDTextWatcher

fun registerViewsEventsForActivity(activity: Activity) {
    val viewMainContainer = activity.window.decorView.findViewById<View>(
        android.R.id.content
    )

    viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener { _, newView ->
        if (newView != null) {
            val idName = if (newView.id == View.NO_ID) {
                "no_id"
            } else {
                newView.resources.getResourceEntryName(newView.id)
            }

            if (newView is EditText) {
                getDataStoreInstance(activity.applicationContext)
                    .saveEvent(
                        NIDEventModel(
                            type = FOCUS,
                            ts = System.currentTimeMillis(),
                            tgs = hashMapOf(
                                "tgs" to idName,
                                "etn" to INPUT,
                                "et" to "text"
                            )
                        ).getOwnJson()
                    )

                getDataStoreInstance(activity.applicationContext)
                    .saveEvent(
                        NIDEventModel(
                            type = WINDOW_RESIZE,
                            ts = System.currentTimeMillis(),
                            tgs = hashMapOf(
                                "tgs" to idName,
                                "etn" to INPUT,
                                "et" to "text"
                            )
                        ).getOwnJson()
                    )

                val textWatcher = NIDTextWatcher(activity.applicationContext, idName)
                newView.removeTextChangedListener(textWatcher)
                newView.addTextChangedListener(textWatcher)
            }
        }
    }
}