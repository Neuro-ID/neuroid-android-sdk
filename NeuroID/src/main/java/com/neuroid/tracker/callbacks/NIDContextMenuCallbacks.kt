package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.neuroid.tracker.events.CONTEXT_MENU
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.events.COPY
import com.neuroid.tracker.events.CUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import org.json.JSONObject

// This is the callback for the context menu that appears when text is already in field
class NIDTextContextMenuCallbacks(
    actionCallBack: ActionMode.Callback?
) : ActionMode.Callback {
    private val wrapper = actionCallBack

    override fun onCreateActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onCreateActionMode(action, menu)
        return true
    }

    override fun onPrepareActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onPrepareActionMode(action, menu)
        return false
    }

    override fun onActionItemClicked(action: ActionMode?, item: MenuItem?): Boolean {
        wrapper?.onActionItemClicked(action, item)

        NIDLog.d(
            "NIDDebug",
            "Existing Text Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }
}

// This is the callback for the context menu that appears when the text field is empty (only available in later API versions)
class NIDLongPressContextMenuCallbacks(
    actionCallBack: ActionMode.Callback?
) : ActionMode.Callback {
    private val wrapper = actionCallBack

    override fun onCreateActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onCreateActionMode(action, menu)
        return true
    }

    override fun onPrepareActionMode(action: ActionMode?, menu: Menu?): Boolean {
        wrapper?.onPrepareActionMode(action, menu)
        return false
    }

    override fun onActionItemClicked(action: ActionMode?, item: MenuItem?): Boolean {
        wrapper?.onActionItemClicked(action, item)

        NIDLog.d(
            "NIDDebug",
            "Long Press Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it, item.toString()) }
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }


}

private fun saveEvent(option: Int, item: String) {
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    val type = when (option) {
        android.R.id.paste -> PASTE
        android.R.id.copy -> COPY
        android.R.id.cut -> CUT
        else -> ""
    }

    val jsonObject = JSONObject()
    jsonObject.put("option", "$option")
    jsonObject.put("item", "$item")
    jsonObject.put("type", "$type")

    if (type.isNotEmpty()) {
        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = CONTEXT_MENU,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    metadata = jsonObject
                )
            )
    }
}