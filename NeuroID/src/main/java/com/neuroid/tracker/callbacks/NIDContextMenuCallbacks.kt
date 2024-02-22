package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.neuroid.tracker.events.CONTEXT_MENU
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.events.COPY
import com.neuroid.tracker.events.CUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.NIDDataStoreManager
import com.neuroid.tracker.utils.NIDLog

abstract class NIDContextMenuCallBacks(
    val dataStore: NIDDataStoreManager,
    actionCallBack: ActionMode.Callback?
) : ActionMode.Callback {
    val wrapper = actionCallBack

    override fun onCreateActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onCreateActionMode(action, menu)
        return true
    }

    override fun onPrepareActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onPrepareActionMode(action, menu)
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }

    internal fun saveEvent(option: Int, item: String) {
        val type = when (option) {
            android.R.id.paste -> PASTE
            android.R.id.copy -> COPY
            android.R.id.cut -> CUT
            else -> ""
        }

        if (type.isNotEmpty()) {
            this.dataStore.saveEvent(
                NIDEventModel(
                    type = CONTEXT_MENU,
                    attrs = listOf(
                        mapOf(
                            "option" to "$option",
                            "item" to item,
                            "type" to type
                        )
                    )
                )
            )
        }
    }
}

// This is the callback for the context menu that appears when text is already in field
class NIDTextContextMenuCallbacks(
    dataStore: NIDDataStoreManager,
    actionCallBack: ActionMode.Callback?
) : NIDContextMenuCallBacks(dataStore, actionCallBack) {

    override fun onActionItemClicked(action: ActionMode?, item: MenuItem?): Boolean {
        wrapper?.onActionItemClicked(action, item)

        NIDLog.d(
            msg="Existing Text Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }
}

// This is the callback for the context menu that appears when the text field is empty (only available in later API versions)
class NIDLongPressContextMenuCallbacks(
    dataStore: NIDDataStoreManager,
    actionCallBack: ActionMode.Callback?
) : NIDContextMenuCallBacks(dataStore, actionCallBack) {
    override fun onActionItemClicked(action: ActionMode?, item: MenuItem?): Boolean {
        wrapper?.onActionItemClicked(action, item)

        NIDLog.d(
           msg="Long Press Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }
}

