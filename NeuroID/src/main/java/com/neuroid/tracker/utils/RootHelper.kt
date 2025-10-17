package com.neuroid.tracker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.InputStreamReader

class RootHelper(
    internal val environmentProvider: NIDEnvironmentProvider = NIDSystemEnvironmentProvider(),
    internal val runtimeProvider: NIDRuntimeProvider = NIDSystemRuntimeProvider(),
    internal val fileCreationUtils: FileCreationUtils = FileCreationUtils(),
    internal val buildTagUtils: NIDTagUtils = NIDTagUtils()) {
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

    internal fun checkForBinary(filename: String): Boolean {
        val pathsArray: List<String> = getPaths()
        var result = false
        for (path in pathsArray) {
            val f = fileCreationUtils.getFile(path, filename)
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

    internal fun getPaths(): List<String> {
        val paths = ArrayList(suPaths)
        val sysPaths = environmentProvider.getenv("PATH")
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
        val buildTags = buildTagUtils.getBuildTags()
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuExists(): Boolean {
        var process: Process? = null
        return try {
            process = runtimeProvider.executeCommand(arrayOf("which", BINARY_SU))
            val `in` = fileCreationUtils.getBufferedReader(InputStreamReader(process.inputStream))
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

    internal fun isAnyPackageFromListInstalled(
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

    internal fun detectPotentiallyDangerousApps(
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
                buildTagUtils.getFingerprint().startsWith("google/sdk_gphone_") &&
                    buildTagUtils.getFingerprint().endsWith(":user/release-keys") &&
                    buildTagUtils.getManufacturer() == "Google" &&
                    buildTagUtils.getProduct().startsWith("sdk_gphone_") &&
                    buildTagUtils.getBrand() == "google" &&
                    buildTagUtils.getModel().startsWith("sdk_gphone_")
            ) || buildTagUtils.getFingerprint().startsWith("generic") ||
                    buildTagUtils.getFingerprint().startsWith("unknown") ||
                    buildTagUtils.getModel().contains("google_sdk") ||
                    buildTagUtils.getModel().contains("Emulator", true) ||
                    buildTagUtils.getDevice().contains("Emulator", true) ||
                    buildTagUtils.getModel().contains("Android SDK built for x86") ||
                    buildTagUtils.getBoard() == "QC_Reference_Phone" &&
                    !buildTagUtils.getManufacturer().equals(
                    "Xiaomi",
                    ignoreCase = true,
                ) ||
                    buildTagUtils.getBoard().lowercase().contains("nox") ||

                    // hardware check for vbox, nox, google
                    buildTagUtils.getHardware() == "goldfish" ||
                    buildTagUtils.getHardware() == "vbox86" ||
                    buildTagUtils.getHardware().lowercase().contains("nox") ||

                    buildTagUtils.getManufacturer().contains("Genymotion") ||

                    // pickup on secondary  manufacturer string for genymotion
                    buildTagUtils.getManufacturer().contains("Genymobile") ||

                    buildTagUtils.getHost().startsWith("Build") ||
                    buildTagUtils.getBrand().startsWith("generic") &&
                    buildTagUtils.getDevice().startsWith("generic") ||

                    // products (looking for vbox, nox and any x86 based emulators on win11, mac intel)
                    buildTagUtils.getProduct() == "google_sdk" ||
                    buildTagUtils.getProduct() == "sdk_x86" ||
                    buildTagUtils.getProduct() == "vbox86p" ||
                    buildTagUtils.getProduct().lowercase().contains("nox") ||

                // sim file check (in case we miss anything above)
                isEmulatorFilesPresent()
        )
    }
}
