package com.sample.neuroid.us.utils

import com.google.gson.InstanceCreator
import kotlinx.coroutines.CoroutineScope
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext

/**
 * GSON needs for parsing objects during testing. No used in app.
 */
class CoroutineScopeAdapter: InstanceCreator<CoroutineScope> {
    override fun createInstance(type: Type?): CoroutineScope {
        return object: CoroutineScope{
            override val coroutineContext: CoroutineContext
                get() = TODO("Not yet implemented")
        }
    }
}