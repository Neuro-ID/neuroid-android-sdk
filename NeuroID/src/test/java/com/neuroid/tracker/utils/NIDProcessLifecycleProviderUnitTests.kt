package com.neuroid.tracker.utils

import androidx.lifecycle.ProcessLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class NIDProcessLifecycleProviderUnitTests {
    @Test
    fun `getProcessLifecycle returns ProcessLifecycleOwner's lifecycle`() {
        val provider = NIDProcessLifecycleProvider()

        assertEquals(ProcessLifecycleOwner.get().lifecycle, provider.getProcessLifecycle())
    }
}



