package com.neuroid.tracker.utils

import java.io.BufferedReader

interface NIDRuntimeProvider {
    fun executeCommand(command: String): Process
    fun executeCommand(command: Array<String>): Process
    fun executeShellCommand(command: Array<String>): BufferedReader?
}

class NIDSystemRuntimeProvider : NIDRuntimeProvider {
    override fun executeCommand(command: String): Process {
        return Runtime.getRuntime().exec(command)
    }

    override fun executeCommand(command: Array<String>): Process {
        return Runtime.getRuntime().exec(command)
    }

    override fun executeShellCommand(command: Array<String>): BufferedReader? {
        return try {
            val process = Runtime.getRuntime().exec(command)
            BufferedReader(java.io.InputStreamReader(process.inputStream))
        } catch (e: Exception) {
            null
        }
    }
}