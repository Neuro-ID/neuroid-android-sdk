package com.neuroid.tracker.utils

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NIDLogTest {
    fun getNIDLog(): NIDLog {
        val logger = mockk<NIDLog>()
        every { logger.v(any(), any()) } just Runs
        every { logger.e(any(), any()) } just Runs
        every { logger.i(any(), any()) } just Runs
        every { logger.w(any(), any()) } just Runs
        every { logger.d(any(), any(), ) } just Runs
        every { logger.nidTag} returns "NID"
        every { logger.errorTag} returns "error"
        every { logger.infoTag} returns "info"
        every { logger.warnTag} returns "warn"
        every { logger.debugTag} returns "debug"
        every { logger.warnTag} returns "warn"
        every { logger.printLine(any(), any(), any() ) } just Runs
        return logger
    }

    @Test
    fun testLoggerVerboseNonRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.v( msg = " test", buildType = "debug")
        verify { logger.v(any(), any()) }
    }

    @Test
    fun testLoggerVerboseRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.v( msg = "test", buildType = "release")
        verify { logger.printLine(any(), any(), any()) }
    }

    @Test
    fun testLoggerErrorNonRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.e( msg = " test", buildType = "debug")
        verify { logger.e(any(), any()) }
    }

    @Test
    fun testLoggerErrorRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.e( msg = "test", buildType = "release")
        verify { logger.printLine(any(), any(), any()) }
    }

    @Test
    fun testLoggerInfoNonRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.i( msg = " test", buildType = "debug")
        verify { logger.i(any(), any()) }
    }

    @Test
    fun testLoggerInfoRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.i( msg = "test", buildType = "release")
        verify { logger.printLine(any(), any(), any()) }
    }

    @Test
    fun testLoggerWarnNonRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.w( msg = " test", buildType = "debug")
        verify { logger.w(any(), any()) }
    }

    @Test
    fun testLoggerWarnRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.w( msg = "test", buildType = "release")
        verify { logger.printLine(any(), any(), any()) }
    }

    @Test
    fun testLoggerDebugNonRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.d( msg = " test", buildType = "debug")
        verify { logger.d(any(), any()) }
    }

    @Test
    fun testLoggerDebugRelease() {
        val logger = getNIDLog()
        val t = NIDLogWrapper(logger)
        t.d( msg = "test", buildType = "release")
        verify { logger.printLine(any(), any(), any()) }
    }
}