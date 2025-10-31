package com.neuroid.tracker.utils

import org.junit.Test

class NIDRuntimeProviderTest {
    @Test
    fun testExecuteCommand() {
        val runtimeProvider = NIDSystemRuntimeProvider()
        val process = runtimeProvider.executeCommand("echo Hello, World!")
        val result = process.inputStream.bufferedReader().readText().trim()
        assert(result == "Hello, World!")
    }

    @Test
    fun testExecuteCommandArray() {
        val runtimeProvider = NIDSystemRuntimeProvider()
        val command = arrayOf("echo", "Hello, World!")
        val process = runtimeProvider.executeCommand(command)
        val result = process.inputStream.bufferedReader().readText().trim()
        assert(result == "Hello, World!")
    }

    @Test
    fun testExecuteCommandShellCommand() {
        val runtimeProvider = NIDSystemRuntimeProvider()
        val command = arrayOf("sh", "-c", "echo Hello, Shell!")
        val reader = runtimeProvider.executeShellCommand(command)
        val result = reader?.readText()?.trim()
        assert(result == "Hello, Shell!")
    }
}