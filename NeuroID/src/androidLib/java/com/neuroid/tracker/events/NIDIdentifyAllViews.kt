package com.neuroid.tracker.events

import android.os.Build
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.forEach
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDTextContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDLongPressContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.models.NIDSensorModel
import com.neuroid.tracker.service.NIDServiceTracker
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.getIdOrTag
import com.neuroid.tracker.utils.getParents
import org.json.JSONArray
import org.json.JSONObject


fun identifyAllViews(
    viewParent: ViewGroup,
    guid: String,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = "",
) {
    NIDLog.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

    viewParent.forEach {
        when (it) {
            is ViewGroup -> {
                identifyAllViews(
                    it,
                    guid,
                    registerTarget,
                    registerListeners,
                    activityOrFragment,
                    parent
                )
                it.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        NIDLog.d(
                            "NIDDebug ChildViewAdded",
                            "ViewAdded: ${child?.getIdOrTag().orEmpty()}"
                        )
                        child?.let { view ->
                            // This is double registering targets and registering listeners before the correct
                            //  lifecycle event which is causing a replay of text input events to occur
//                         identifyView(view, guid, registerTarget, registerListeners)
                        }
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        NIDLog.d(
                            "NIDDebug ViewListener",
                            "ViewRemoved: ${child?.getIdOrTag().orEmpty()}"
                        )
                    }
                })
            }
            else -> {
                identifyView(
                    it,
                    guid,
                    registerTarget,
                    registerListeners,
                    activityOrFragment,
                    parent
                )
            }
        }

        // exception groups that should be registered:
        when (it) {
            is RadioGroup -> {
                identifyView(
                    it,
                    guid,
                    registerTarget,
                    registerListeners,
                    activityOrFragment,
                    parent
                )
            }
        }
    }
}

fun identifyView(
    view: View,
    guid: String,
    registerTarget: Boolean = true,
    registerListeners: Boolean = true,
    activityOrFragment: String = "",
    parent: String = "",
) {
    when (view) {
        is ViewGroup -> {
            identifyAllViews(
                view,
                guid,
                registerTarget,
                registerListeners,
                activityOrFragment = activityOrFragment,
                parent = parent
            )
        }
        else -> {
            if (registerTarget) {
                registerComponent(
                    view,
                    guid,
                    activityOrFragment = activityOrFragment,
                    parent = parent
                )
            }
            if (registerListeners) {
                registerListeners(view)
            }
        }
    }

    // exception groups that should be registered:
    when (view) {
        is RadioGroup -> {
            if (registerTarget) {
                registerComponent(
                    view,
                    guid,
                    activityOrFragment = activityOrFragment,
                    parent = parent
                )
            }
            if (registerListeners) {
                registerListeners(view)
            }
        }
    }
}


fun registerComponent(
    view: View,
    guid: String,
    rts: String? = null,
    activityOrFragment: String = "",
    parent: String = "",
) {
    NIDLog.d(
        "NIDDebug registeredComponent",
        "view: ${view::class} java: ${view.javaClass.simpleName}"
    )

    val idName = view.getIdOrTag()
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()
    var et = ""
    var v = "S~C~~0"
    val metaData = JSONObject()

    when (view) {
        is EditText -> {
            et = "Edittext"
            v = "S~C~~${view.text.length}"
        }
//        is CheckBox, is AppCompatCheckBox -> {
//            et = "CheckBox"
//        }
        is RadioButton -> {
            et = "RadioButton"
            v = "${view.isChecked}"

            metaData.put("type", "radioButton")
            metaData.put("id", "${view.getIdOrTag()}")

            // go up to 3 parents in case a RadioGroup is not the direct parent
            var rParent = view.parent;
            repeat(3) { index ->
                if (rParent is RadioGroup) {
                    val p = rParent as RadioGroup
                    metaData.put("rGroupId", "${p.getIdOrTag()}")
                    return@repeat
                } else {
                    rParent = rParent.parent
                }
            }
        }
        is RadioGroup -> {
            et = "RadioGroup"
            v = "${view.checkedRadioButtonId}"
        }
        is ToggleButton -> {
            et = "ToggleButton"
        }
        is Switch, is SwitchCompat -> {
            et = "Switch"
        }
        is Button, is ImageButton -> {
            et = "Button"
        }
        is SeekBar -> {
            et = "SeekBar"
        }
        is Spinner -> {
            et = "Spinner"
        }
        is RatingBar -> {
            et = "RatingBar"
        }
    }

    NIDLog.d("NIDDebug et at registerComponent", "$et")

    // early exit if not supported target type
    if (et.isEmpty()) {
        return
    }

//    NIDLog.d(
//        "NID-Activity",
//        "Actually Target Registered $et - ${view.javaClass.simpleName} - ${view::class} - ${view.getIdOrTag()} $parent $activityOrFragment"
//    )
    val pathFrag = if (NIDServiceTracker.screenFragName.isEmpty()) {
        ""
    } else {
        "/${NIDServiceTracker.screenFragName}"
    }
    val urlView = ANDROID_URI + NIDServiceTracker.screenActivityName + "$pathFrag/" + idName

    val idJson = JSONObject().put("n", "guid").put("v", guid)
    val classJson = JSONObject().put("n", "screenHierarchy")
        .put("v", "${view.getParents()}${NIDServiceTracker.screenName}")
    val parentData =
        JSONObject().put("parentClass", "$parent").put("component", "$activityOrFragment")

    val attrJson = JSONArray().put(idJson).put(classJson).put(parentData).put(metaData)


    getDataStoreInstance()
        .saveEvent(
            NIDEventModel(
                type = REGISTER_TARGET,
                attrs = attrJson,
                tg = mapOf("attr" to attrJson),
                et = et + "::" + view.javaClass.simpleName,
                etn = "INPUT",
                ec = NIDServiceTracker.screenName,
                eid = idName,
                tgs = idName,
                en = idName,
                v = v,
                hv = v.getSHA256withSalt()?.take(8),
                ts = System.currentTimeMillis(),
                url = urlView,
                gyro = gyroData,
                accel = accelData,
                rts = rts
            )
        )
}

// list of text watchers in the entire app
val textWatchers = mutableListOf<TextWatcher>()

private fun registerListeners(view: View) {
    val idName = view.getIdOrTag()
    val simpleClassName = view.javaClass.simpleName
    val gyroData = NIDSensorHelper.getGyroscopeInfo()
    val accelData = NIDSensorHelper.getAccelerometerInfo()

    // EditText is a parent class to multiple components
    if (view is EditText) {
        NIDLog.d(
            "NID-Activity",
            "EditText Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}"
        )
        // add Text Change watcher
        val textWatcher = NIDTextWatcher(idName, simpleClassName)
        // first we have to clear the text watcher that is currently in the EditText
        for(watcher in textWatchers) {
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
                    simpleClassName,
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
                    simpleClassName,
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
                    simpleClassName,
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

//            NIDLog.d(
//                "NID-Activity",
//                "Select Selected $idName - $simpleClassName $position $p3 $viewList"
//            )
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
    simpleClassName: String,
    gyroData: NIDSensorModel?,
    accelData: NIDSensorModel?,
): AdapterView.OnItemClickListener {
    return AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
        lastClickListener?.onItemClick(adapter, viewList, position, p3)

//        NIDLog.d(
//            "NID-Activity",
//            "Select CLICK $idName - $simpleClassName $position $p3 $viewList"
//        )
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