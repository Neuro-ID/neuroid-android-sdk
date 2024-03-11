package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.CONTEXT_MENU
import com.neuroid.tracker.events.COPY
import com.neuroid.tracker.events.CUT
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.utils.NIDLogWrapper

abstract class NIDContextMenuCallBacks(
    val neuroID: NeuroID,
    actionCallBack: ActionMode.Callback?,
) : ActionMode.Callback {
    val wrapper = actionCallBack

    override fun onCreateActionMode(
        action: ActionMode?,
        menu: Menu?,
    ): Boolean {
        wrapper?.onCreateActionMode(action, menu)
        return true
    }

    override fun onPrepareActionMode(
        action: ActionMode?,
        menu: Menu?,
    ): Boolean {
        wrapper?.onPrepareActionMode(action, menu)
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }

    internal fun saveEvent(
        option: Int,
        item: String,
    ) {
        val type =
            when (option) {
                android.R.id.paste -> PASTE
                android.R.id.copy -> COPY
                android.R.id.cut -> CUT
                else -> ""
            }

        if (type.isNotEmpty()) {
            neuroID.captureEvent(
                type = CONTEXT_MENU,
                attrs =
                    listOf(
                        mapOf(
                            "option" to "$option",
                            "item" to item,
                            "type" to type,
                        ),
                    ),
            )
        }
    }
}

// This is the callback for the context menu that appears when text is already in field
class NIDTextContextMenuCallbacks(
    neuroID: NeuroID,
    val logger: NIDLogWrapper,
    actionCallBack: ActionMode.Callback?,
) : NIDContextMenuCallBacks(neuroID, actionCallBack) {
    override fun onActionItemClicked(
        action: ActionMode?,
        item: MenuItem?,
    ): Boolean {
        wrapper?.onActionItemClicked(action, item)

        logger.d(msg = "Existing Text Action Context - $item")

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }
}

// This is the callback for the context menu that appears when the text field is empty (only
// available in later API versions)
class NIDLongPressContextMenuCallbacks(
    neuroID: NeuroID,
    val logger: NIDLogWrapper,
    actionCallBack: ActionMode.Callback?,
) : NIDContextMenuCallBacks(neuroID, actionCallBack) {
    override fun onActionItemClicked(
        action: ActionMode?,
        item: MenuItem?,
    ): Boolean {
        wrapper?.onActionItemClicked(action, item)

        logger.d(msg = "Long Press Action Context - $item")

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }
}
