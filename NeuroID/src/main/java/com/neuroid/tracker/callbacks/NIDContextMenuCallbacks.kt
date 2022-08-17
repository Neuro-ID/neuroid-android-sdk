package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.neuroid.tracker.events.*
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance

class NIDContextMenuCallbacks(
    actionCallBack: ActionMode.Callback?
): ActionMode.Callback {
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
        item?.itemId?.let { saveEvent(it) }

        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }

    private fun saveEvent(option: Int) {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val type = when(option) {
            android.R.id.paste -> PASTE
            android.R.id.copy -> COPY
            android.R.id.cut -> CUT
            else -> ""
        }

        if (type.isNotEmpty()) {
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = type,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData
                    )
                )
        }
    }

}