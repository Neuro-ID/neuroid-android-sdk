package com.neuroid.tracker.utils

import com.google.gson.InstanceCreator
import kotlinx.coroutines.CoroutineScope
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext

class CoroutineScopeAdapter: InstanceCreator<CoroutineScope> {
    override fun createInstance(type: Type?): CoroutineScope {
        return object: CoroutineScope{
            override val coroutineContext: CoroutineContext
                get() = TODO("Not yet implemented")
        }
    }
}