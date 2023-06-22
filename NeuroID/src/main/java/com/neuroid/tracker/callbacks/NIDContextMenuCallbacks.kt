package com.neuroid.tracker.callbacks

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.neuroid.tracker.events.PASTE
import com.neuroid.tracker.events.COPY
import com.neuroid.tracker.events.CUT
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog

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
            "NID-a",
            "Existing Text Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it) }

        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }

    private fun saveEvent(option: Int) {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val type = when (option) {
            android.R.id.paste -> PASTE
            android.R.id.copy -> COPY
            android.R.id.cut -> CUT
            else -> ""
        }

        NIDLog.d("NID-a", "Existing Context Save: $option $type")

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
            "NID-a",
            "Long Press Action Context - $item"
        )

        item?.itemId?.let { saveEvent(it) }

        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        wrapper?.onDestroyActionMode(actionMode)
    }

    private fun saveEvent(option: Int) {
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        val type = when (option) {
            android.R.id.paste -> PASTE
            android.R.id.copy -> COPY
            android.R.id.cut -> CUT
            else -> ""
        }

        NIDLog.d("NID-a", "Long Press Context Save: $option $type")

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