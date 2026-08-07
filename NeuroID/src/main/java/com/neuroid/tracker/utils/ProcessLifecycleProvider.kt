package com.neuroid.tracker.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Thin, mockable wrapper around [ProcessLifecycleOwner]. `ProcessLifecycleOwner.get()` requires a
 * main-thread `Looper` to be present (via `ArchTaskExecutor`), which does not exist in plain JVM
 * unit tests (this SDK does not use Robolectric). Production code should always go through this
 * wrapper - never call `ProcessLifecycleOwner.get()` directly - so tests can substitute a fake
 * `Lifecycle` instead.
 */
internal open class ProcessLifecycleProvider {
    open fun getProcessLifecycle(): Lifecycle = ProcessLifecycleOwner.get().lifecycle
}
