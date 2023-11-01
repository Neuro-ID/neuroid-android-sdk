package com.neuroid.tracker.events

import android.os.Build
import android.text.TextWatcher
import android.view.View
import android.widget.AbsSpinner
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import androidx.annotation.RequiresApi
import com.neuroid.tracker.callbacks.NIDLongPressContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.callbacks.NIDTextContextMenuCallbacks
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTextWatcher

// list of text watchers in the entire app
val textWatchers = mutableListOf<TextWatcher>()

fun registerListeners(view: View, logger: NIDLogWrapper) {
    val idName = view.getIdOrTag()
    val simpleClassName = view.javaClass.simpleName
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    // EditText is a parent class to multiple components
    if (view is EditText) {
        logger.d(
            "NID-Activity",
            "EditText Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}"
        )
        // add Text Change watcher
        val textWatcher = NIDTextWatcher(idName, simpleClassName)
        // first we have to clear the text watcher that is currently in the EditText
        for (watcher in textWatchers) {
            view.removeTextChangedListener(watcher)
        }
        // we add the new one in there
        view.addTextChangedListener(textWatcher)
        // we add the new one to the list of existing text watchers so we can remove it later when
        // it is re-registered
        textWatchers.add(textWatcher)

        // add original action menu watcher
        val actionCallback = view.customSelectionActionModeCallback
        if (actionCallback !is NIDTextContextMenuCallbacks) {
            view.customSelectionActionModeCallback = NIDTextContextMenuCallbacks(actionCallback)
        }

        // if later api version, add additional action menu watcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addExtraActionMenuListener(view)
        }
    }

    // additional subclasses to be captured
    when (view) {
        is AbsSpinner -> {
            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }

        is Spinner -> {
            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }

        is AutoCompleteTextView -> {
            val lastClickListener = view.onItemClickListener
            view.onItemClickListener = null
            view.onItemClickListener =
                addSelectOnClickListener(
                    idName,
                    lastClickListener,
                    gyroData,
                    accelData
                )

            val lastSelectListener = view.onItemSelectedListener
            view.onItemSelectedListener = null
            view.onItemSelectedListener = addSelectOnSelect(
                idName,
                lastSelectListener,
                simpleClassName,
                gyroData,
                accelData
            )
        }
    }
}

private fun addSelectOnSelect(
    idName: String,
    lastSelectListener: AdapterView.OnItemSelectedListener?,
    simpleClassName: String,
    gyroData: NIDSensorModel?,
    accelData: NIDSensorModel?,
): AdapterView.OnItemSelectedListener {
    return object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            adapter: AdapterView<*>?,
            viewList: View?,
            position: Int,
            p3: Long
        ) {
            lastSelectListener?.onItemSelected(adapter, viewList, position, p3)
            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = SELECT_CHANGE,
                        tg = hashMapOf(
                            "etn" to simpleClassName,
                            "tgs" to idName,
                            "sender" to simpleClassName
                        ),
                        tgs = idName,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData,
                        v = "$position"
                    )
                )
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            lastSelectListener?.onNothingSelected(p0)
        }
    }
}

private fun addSelectOnClickListener(
    idName: String,
    lastClickListener: AdapterView.OnItemClickListener?,
    gyroData: NIDSensorModel?,
    accelData: NIDSensorModel?,
): AdapterView.OnItemClickListener {
    return AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
        lastClickListener?.onItemClick(adapter, viewList, position, p3)

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = SELECT_CHANGE,
                    tg = hashMapOf(
                        "etn" to "INPUT",
                        "et" to "text"
                    ),
                    tgs = idName,
                    ts = System.currentTimeMillis(),
                    gyro = gyroData,
                    accel = accelData,
                    v = "$position"
                )
            )
    }
}
@RequiresApi(Build.VERSION_CODES.M)
private fun addExtraActionMenuListener(view: EditText) {
    val actionInsertionCallback = view.customInsertionActionModeCallback
    if (actionInsertionCallback !is NIDLongPressContextMenuCallbacks) {
        view.customInsertionActionModeCallback =
            NIDLongPressContextMenuCallbacks(actionInsertionCallback)
    }
}

