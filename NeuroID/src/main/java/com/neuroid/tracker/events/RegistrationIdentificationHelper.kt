package com.neuroid.tracker.events

import android.app.Activity
import android.os.Build
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSpinner
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.core.view.forEach
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.callbacks.NIDGlobalEventCallback
import com.neuroid.tracker.callbacks.NIDLongPressContextMenuCallbacks
import com.neuroid.tracker.callbacks.NIDTextContextMenuCallbacks
import com.neuroid.tracker.extensions.getIdOrTag
import com.neuroid.tracker.extensions.getParentActivity
import com.neuroid.tracker.extensions.getParentFragment
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.NIDTextWatcher
import com.neuroid.tracker.utils.handleIdentifyAllViews
import com.neuroid.tracker.utils.verifyComponentType
import java.util.UUID

// list of text watchers in the entire app
val textWatchers = mutableListOf<TextWatcher>()

class RegistrationIdentificationHelper(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
) {
    val additionalListeners: AdditionalListeners = AdditionalListeners(neuroID, logger)
    val singleTargetListenerRegister: SingleTargetListenerRegister =
        SingleTargetListenerRegister(neuroID, logger, additionalListeners)

    fun registerTargetFromScreen(
        activity: Activity,
        registerTarget: Boolean = true,
        registerListeners: Boolean = true,
        activityOrFragment: String = "",
        parent: String = "",
    ) {
        // DEBUG are we actually fetching all view containers
        val viewMainContainer =
            activity.window.decorView.findViewById<View>(
                android.R.id.content,
            ) as ViewGroup

        val hashCodeAct = activity.hashCode()
        val guid = UUID.nameUUIDFromBytes(hashCodeAct.toString().toByteArray()).toString()

        handleIdentifyAllViews {
            identifyAllViews(
                viewMainContainer,
                guid,
                registerTarget,
                registerListeners,
                activityOrFragment,
                parent,
            )
        }
    }

    fun registerWindowListeners(activity: Activity) {
        val viewMainContainer =
            activity.window.decorView.findViewById<View>(
                android.R.id.content,
            )

        val callBack = activity.window.callback

        if (callBack !is NIDGlobalEventCallback) {
            val nidGlobalEventCallback =
                NIDGlobalEventCallback(
                    callBack,
                    TouchEventManager(
                        viewMainContainer as ViewGroup,
                        neuroID,
                        logger,
                    ),
                    viewMainContainer,
                    neuroID,
                    logger,
                    singleTargetListenerRegister,
                )
            viewMainContainer.viewTreeObserver.addOnGlobalFocusChangeListener(nidGlobalEventCallback)
            viewMainContainer.viewTreeObserver.addOnGlobalLayoutListener(
                nidGlobalEventCallback,
            )

            activity.window.callback = nidGlobalEventCallback
        }
    }

    fun registerSingleTargetListeners(
        view: View,
        guid: String,
        registerTarget: Boolean = true,
        registerListeners: Boolean = true,
        activityOrFragment: String = "",
        parent: String = "",
    ) {
        if (registerTarget) {
            singleTargetListenerRegister.registerComponent(
                view,
                guid,
                activityOrFragment = activityOrFragment,
                parent = parent,
            )
        }
        if (registerListeners) {
            singleTargetListenerRegister.registerListeners(view)
        }
    }

    fun identifyAllViews(
        viewParent: ViewGroup,
        guid: String,
        registerTarget: Boolean = true,
        registerListeners: Boolean = true,
        activityOrFragment: String = "",
        parent: String = "",
    ) {
        logger.d("NIDDebug identifyAllViews", "viewParent: ${viewParent.getIdOrTag()}")

        viewParent.forEach {
            var shouldRegister = false
            when (it) {
                is ViewGroup -> {
                    identifyAllViews(
                        it,
                        guid,
                        registerTarget,
                        registerListeners,
                        activityOrFragment,
                        parent,
                    )

                    it.setOnHierarchyChangeListener(additionalListeners.addOnHierarchyChangeListener())

                    // exception groups that should be registered:
                    if (it is RadioGroup) {
                        shouldRegister = true
                    }
                }

                else -> {
                    shouldRegister = true
                }
            }

            if (shouldRegister) {
                identifySingleView(
                    it,
                    guid,
                    registerTarget,
                    registerListeners,
                    activityOrFragment,
                    parent,
                )
            }
        }
    }

    fun identifySingleView(
        view: View,
        guid: String,
        registerTarget: Boolean = true,
        registerListeners: Boolean = true,
        activityOrFragment: String = "",
        parent: String = "",
    ) {
        var shouldRegister = false
        when (view) {
            is ViewGroup -> {
                identifyAllViews(
                    view,
                    guid,
                    registerTarget,
                    registerListeners,
                    activityOrFragment = activityOrFragment,
                    parent = parent,
                )

                // exception groups that should be registered:
                if (view is RadioGroup) {
                    shouldRegister = true
                }
            }
            else -> {
                shouldRegister = true
            }
        }

        if (shouldRegister) {
            registerSingleTargetListeners(
                view,
                guid,
                registerTarget,
                registerListeners,
                activityOrFragment = activityOrFragment,
                parent = parent,
            )
        }
    }
}

class SingleTargetListenerRegister(
    val neuroID: NeuroID,
    val logger: NIDLogWrapper,
    val additionalListeners: AdditionalListeners,
) {
    fun registerListeners(view: View) {
        val idName = view.getIdOrTag()
        val simpleClassName = view.javaClass.simpleName

        // EditText is a parent class to multiple components
        if (view is EditText) {
            logger.d(
                "NID-Activity",
                "EditText Listener $simpleClassName - ${view::class} - ${view.getIdOrTag()}",
            )
            // add Text Change watcher
            val textWatcher = NIDTextWatcher(neuroID, logger, idName, simpleClassName)
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
                view.customSelectionActionModeCallback =
                    NIDTextContextMenuCallbacks(
                        neuroID,
                        logger,
                        actionCallback,
                    )
            }

            // if later api version, add additional action menu watcher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                additionalListeners.addExtraActionMenuListener(view)
            }
        }

        // additional subclasses to be captured
        when (view) {
            is AutoCompleteTextView -> {
                val lastClickListener = view.onItemClickListener
                view.onItemClickListener = null
                view.onItemClickListener =
                    additionalListeners.addSelectOnClickListener(
                        idName,
                        lastClickListener,
                    )

                val lastSelectListener = view.onItemSelectedListener
                view.onItemSelectedListener = null
                view.onItemSelectedListener =
                    additionalListeners.addSelectOnSelect(
                        idName,
                        lastSelectListener,
                        simpleClassName,
                    )
            }

            is Spinner -> {
                val lastClickListener = view.onItemClickListener
                view.onItemClickListener = null
                view.onItemClickListener =
                    additionalListeners.addSelectOnClickListener(
                        idName,
                        lastClickListener,
                    )

                val lastSelectListener = view.onItemSelectedListener
                view.onItemSelectedListener = null
                view.onItemSelectedListener =
                    additionalListeners.addSelectOnSelect(
                        idName,
                        lastSelectListener,
                        simpleClassName,
                    )
            }

            is AbsSpinner -> {
                val lastClickListener = view.onItemClickListener
                view.onItemClickListener = null
                view.onItemClickListener =
                    additionalListeners.addSelectOnClickListener(
                        idName,
                        lastClickListener,
                    )

                val lastSelectListener = view.onItemSelectedListener
                view.onItemSelectedListener = null
                view.onItemSelectedListener =
                    additionalListeners.addSelectOnSelect(
                        idName,
                        lastSelectListener,
                        simpleClassName,
                    )
            }
        }
    }

    fun registerComponent(
        view: View,
        guid: String,
        rts: String? = null,
        activityOrFragment: String = "",
        parent: String = "",
        onComplete: () -> Unit = {},
    ) {
        val simpleName = view.javaClass.simpleName

        logger.d(
            "NIDDebug registeredComponent",
            "view: ${view::class} java: $simpleName",
        )

        val (idName, et, v, metaData) = verifyComponentType(view)

        logger.d("NIDDebug et at registerComponent", et)

        // early exit if not supported target type
        if (et.isEmpty()) {
            return
        }

        val attrJson =
            createAtrrList(
                view,
                guid,
                idName,
                activityOrFragment,
                parent,
            )
        attrJson.add(metaData)

        registerFinalComponent(
            rts,
            idName,
            et,
            v,
            simpleName,
            attrJson,
        ) {
            onComplete()
        }
    }

    fun registerFinalComponent(
        rts: String? = null,
        idName: String,
        et: String,
        v: String,
        simpleName: String,
        attrJson: List<Map<String, Any>>,
        onComplete: () -> Unit = {},
    ) {
        val pathFrag =
            if (NeuroID.screenFragName.isEmpty()) {
                ""
            } else {
                "/${NeuroID.screenFragName}"
            }

        val urlView = ANDROID_URI + NeuroID.screenActivityName + "$pathFrag/" + idName

        logger.d("NID test output", "etn: INPUT, et: $simpleName, eid: $idName, v:$v")

        neuroID.captureEvent(
            type = REGISTER_TARGET,
            attrs = attrJson,
            tg = mapOf("attr" to attrJson),
            et = "$et::$simpleName",
            etn = "INPUT",
            ec = NeuroID.screenName,
            eid = idName,
            tgs = idName,
            en = idName,
            v = v,
            hv = v.getSHA256withSalt().take(8),
            url = urlView,
            rts = rts,
        )

        onComplete()
    }

    fun createAtrrList(
        view: View,
        guid: String,
        idName: String,
        activityOrFragment: String = "",
        parent: String = "",
    ): MutableList<Map<String, Any>> {
        val idJson =
            mapOf<String, Any>(
                "n" to "guid",
                "v" to guid,
            )

        val parentActivity = view.getParentActivity()
        val parentFragment = view.getParentFragment()

        val screenHierarchy =
            mapOf<String, Any>(
                "n" to "screenHierarchy",
                "v" to "/$parentActivity/${parentFragment ?: ""}$idName",
            )

        val topScreenHierarchy =
            mapOf<String, Any>(
                "n" to "top-screenHierarchy",
                "v" to "/$parent/$idName",
            )

        val parentClassData =
            mapOf<String, Any>(
                "n" to "parentClass",
                "v" to parent,
            )

        val parentComponentData =
            mapOf<String, Any>(
                "n" to "component",
                "v" to activityOrFragment,
            )

        return mutableListOf(
            idJson,
            screenHierarchy,
            topScreenHierarchy,
            parentClassData,
            parentComponentData,
        )
    }
}

class AdditionalListeners(val neuroID: NeuroID, val logger: NIDLogWrapper) {
    internal fun addSelectOnSelect(
        idName: String,
        lastSelectListener: AdapterView.OnItemSelectedListener?,
        simpleClassName: String,
    ): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapter: AdapterView<*>?,
                viewList: View?,
                position: Int,
                p3: Long,
            ) {
                lastSelectListener?.onItemSelected(adapter, viewList, position, p3)
                neuroID.captureEvent(
                    type = SELECT_CHANGE,
                    tg =
                        hashMapOf(
                            "etn" to simpleClassName,
                            "tgs" to idName,
                            "sender" to simpleClassName,
                        ),
                    tgs = idName,
                    v = "$position",
                )
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                lastSelectListener?.onNothingSelected(p0)
            }
        }
    }

    internal fun addSelectOnClickListener(
        idName: String,
        lastClickListener: AdapterView.OnItemClickListener?,
    ): AdapterView.OnItemClickListener {
        return AdapterView.OnItemClickListener { adapter, viewList, position, p3 ->
            lastClickListener?.onItemClick(adapter, viewList, position, p3)

            neuroID.captureEvent(
                type = SELECT_CHANGE,
                tg =
                    hashMapOf(
                        "etn" to "INPUT",
                        "et" to "text",
                    ),
                tgs = idName,
                v = "$position",
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun addExtraActionMenuListener(view: EditText) {
        val actionInsertionCallback = view.customInsertionActionModeCallback
        if (actionInsertionCallback !is NIDLongPressContextMenuCallbacks) {
            view.customInsertionActionModeCallback =
                NIDLongPressContextMenuCallbacks(
                    neuroID,
                    logger,
                    actionInsertionCallback,
                )
        }
    }

    internal fun addOnHierarchyChangeListener(): ViewGroup.OnHierarchyChangeListener {
        return object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(
                parent: View?,
                child: View?,
            ) {
                logger.d(
                    "NIDDebug ChildViewAdded",
                    "ViewAdded: ${child?.getIdOrTag().orEmpty()}",
                )
                child?.let { view ->
                    // This is double registering targets and registering listeners before the correct
                    //  lifecycle event which is causing a replay of text input events to occur
//                         identifyView(view, guid, registerTarget, registerListeners)
                }
            }

            override fun onChildViewRemoved(
                parent: View?,
                child: View?,
            ) {
                logger.d(
                    "NIDDebug ViewListener",
                    "ViewRemoved: ${child?.getIdOrTag().orEmpty()}",
                )
            }
        }
    }
}
