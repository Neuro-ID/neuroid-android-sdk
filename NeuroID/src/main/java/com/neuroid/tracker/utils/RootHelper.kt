package com.neuroid.tracker.utils

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootHelper {

    private companion object {
        const val BINARY_SU = "su"
        const val BINARY_BUSYBOX = "busybox"
        val suPaths = listOf(
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/cache/",
            "/data/",
            "/dev/"
        )
    }


    fun isRooted(): Boolean {
        return checkForBinary(BINARY_SU) || detectTestKeys() || checkForBinary(BINARY_BUSYBOX) || checkSuExists()
    }


    private fun checkForBinary(filename: String): Boolean {
        val pathsArray: List<String> = getPaths()
        var result = false
        for (path in pathsArray) {
            val f = File(path, filename)
            val fileExists = f.exists()
            if (fileExists) {
                result = true
            }
        }
        return result
    }

    private fun getPaths(): List<String> {
        val paths = ArrayList(suPaths)
        val sysPaths = System.getenv("PATH")
        if (sysPaths == null || sysPaths.isEmpty()) {
            return listOf(sysPaths ?: "")
        }
        sysPaths.split(":").forEach { path ->
            var auxPath = ""
            if (!path.endsWith("/")) {
                auxPath = "$path/"
            }
            if (!suPaths.contains(auxPath)) {
                paths.add(auxPath)
            }
        }
        return paths
    }


    private fun detectTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuExists(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", BINARY_SU))
            val `in` = BufferedReader(InputStreamReader(process.inputStream))
            `in`.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

}