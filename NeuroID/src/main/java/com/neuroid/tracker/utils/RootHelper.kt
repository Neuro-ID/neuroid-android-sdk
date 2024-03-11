package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootHelper {
    internal companion object {
        const val BINARY_SU = "su"
        const val BINARY_BUSYBOX = "busybox"

        var emulatorFiles =
            listOf(
                File("ueventd.android_x86.rc"),
                File("x86.prop"),
                File("ueventd.ttVM_x86.rc"),
                File("init.ttVM_x86.rc"),
                File("fstab.ttVM_x86"),
                File("fstab.vbox86"),
                File("init.vbox86.rc"),
                File("ueventd.vbox86.rc"),
                File("fstab.andy"),
                File("ueventd.andy.rc"),
                File("fstab.nox"),
                File("init.nox.rc"),
                File("ueventd.nox.rc"),
                File("/dev/socket/genyd"),
                File("/dev/socket/baseband_genyd"),
                File("/dev/socket/qemud"),
                File("/dev/qemu_pipe"),
            )

        val suPaths =
            listOf(
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
                "/dev/",
            )
        val knownRootAppsPackages =
            listOf(
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
                "com.topjohnwu.magisk",
                "com.kingroot.kinguser",
                "com.kingo.root",
                "com.smedialink.oneclickroot",
                "com.zhiqupk.root.global",
                "com.alephzain.framaroot",
            )
        val knownDangerousAppsPackages =
            listOf(
                "com.koushikdutta.rommanager",
                "com.koushikdutta.rommanager.license",
                "com.dimonvideo.luckypatcher",
                "com.chelpus.lackypatch",
                "com.ramdroid.appquarantine",
                "com.ramdroid.appquarantinepro",
                "com.android.vending.billing.InAppBillingService.COIN",
                "com.android.vending.billing.InAppBillingService.LUCK",
                "com.chelpus.luckypatcher",
                "com.blackmartalpha",
                "org.blackmart.market",
                "com.allinone.free",
                "com.repodroid.app",
                "org.creeplays.hack",
                "com.baseappfull.fwd",
                "com.zmapp",
                "com.dv.marketmod.installer",
                "org.mobilism.android",
                "com.android.wp.net.log",
                "com.android.camera.update",
                "cc.madkite.freedom",
                "com.solohsu.android.edxp.manager",
                "org.meowcat.edxposed.manager",
                "com.xmodgame",
                "com.cih.game_cih",
                "com.charles.lpoqasert",
                "catch_.me_.if_.you_.can_",
            )
    }

    fun isRooted(context: Context): Boolean {
        return detectRootManagementApps(context) || detectPotentiallyDangerousApps(context) ||
            checkForBinary(BINARY_SU) || detectTestKeys() ||
            checkForBinary(BINARY_BUSYBOX) || checkSuExists() || checkForMagiskBinary()
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

    private fun isEmulatorFilesPresent(): Boolean {
        emulatorFiles.forEach { path ->
            if (path.exists()) {
                return true
            }
        }
        return false
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

    private fun detectRootManagementApps(
        context: Context,
        additionalRootManagementApps: List<String> = emptyList(),
    ): Boolean {
        val packages = ArrayList<String>()
        packages.addAll(knownRootAppsPackages)
        packages.addAll(additionalRootManagementApps)
        return isAnyPackageFromListInstalled(context, packages)
    }

    private fun isAnyPackageFromListInstalled(
        context: Context,
        packages: List<String>,
    ): Boolean {
        var result = false
        val pm: PackageManager = context.packageManager
        for (packageName in packages) {
            try {
                // Root app detected
                pm.getPackageInfo(packageName, 0)
                result = true
            } catch (e: PackageManager.NameNotFoundException) {
                // Exception thrown, package is not installed into the system
            }
        }
        return result
    }

    private fun detectPotentiallyDangerousApps(
        context: Context,
        additionalDangerousApps: List<String> = emptyList(),
    ): Boolean {
        val packages = ArrayList<String>()
        packages.addAll(knownDangerousAppsPackages)
        packages.addAll(additionalDangerousApps)
        return isAnyPackageFromListInstalled(context, packages)
    }

    private fun checkForMagiskBinary() = checkForBinary("magisk")

    fun isProbablyEmulator(): Boolean {
        return (
            (
                Build.FINGERPRINT.startsWith("google/sdk_gphone_") &&
                    Build.FINGERPRINT.endsWith(":user/release-keys") &&
                    Build.MANUFACTURER == "Google" && Build.PRODUCT.startsWith("sdk_gphone_") &&
                    Build.BRAND == "google" &&
                    Build.MODEL.startsWith("sdk_gphone_")
            ) ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator", true) ||
                Build.DEVICE.contains("Emulator", true) ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.BOARD == "QC_Reference_Phone" &&
                !Build.MANUFACTURER.equals(
                    "Xiaomi",
                    ignoreCase = true,
                ) ||
                Build.BOARD.lowercase().contains("nox") ||

                // hardware check for vbox, nox, google
                Build.HARDWARE == "goldfish" ||
                Build.HARDWARE == "vbox86" ||
                Build.HARDWARE.lowercase().contains("nox") ||

                Build.MANUFACTURER.contains("Genymotion") ||

                // pickup on secondary  manufacturer string for genymotion
                Build.MANUFACTURER.contains("Genymobile") ||

                Build.HOST.startsWith("Build") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||

                // products (looking for vbox, nox and any x86 based emulators on win11, mac intel)
                Build.PRODUCT == "google_sdk" ||
                Build.PRODUCT == "sdk_x86" ||
                Build.PRODUCT == "vbox86p" ||
                Build.PRODUCT.lowercase().contains("nox") ||

                // sim file check (in case we miss anything above)
                isEmulatorFilesPresent()
        )
    }
}
