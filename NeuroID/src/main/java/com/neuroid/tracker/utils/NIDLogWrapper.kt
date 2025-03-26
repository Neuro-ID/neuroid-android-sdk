package com.neuroid.tracker.utils

import com.neuroid.tracker.BuildConfig

class NIDLogWrapper(val logger: NIDLog = NIDLog()) {
    fun d(
        tag: String? = null,
        msg: String,
        buildType: String = BuildConfig.BUILD_TYPE,
        cb: () -> String = { msg },
    ) {
        cb().chunked(900).forEach {
            if (buildType.equals("release")) {
                logger.printLine(tag, logger.debugTag, it)
            } else {
                logger.d(tag, it)
            }
        }
    }

    fun e(
        tag: String? = null,
        msg: String,
        buildType: String = BuildConfig.BUILD_TYPE,
        cb: () -> String = { msg },
    ) {
        cb().chunked(900).forEach {
            if (buildType.equals("release")) {
                logger.printLine(tag, logger.errorTag, it)
            } else {
                logger.e(tag, it)
            }
        }
    }

    fun i(
        tag: String? = null,
        msg: String,
        buildType: String = BuildConfig.BUILD_TYPE,
        cb: () -> String = { msg }
    ) {
        cb().chunked(900).forEach {
            if (buildType.equals("release")) {
                logger.printLine(tag, logger.infoTag, it)
            } else {
                logger.i(tag, it)
            }
        }
    }

    fun v(
        tag: String? = null,
        msg: String,
        buildType: String = BuildConfig.BUILD_TYPE,
        cb: () -> String = { msg }
    ) {
        cb().chunked(900).forEach {
            if (buildType.equals("release")) {
                logger.printLine(tag, logger.nidTag, it)
            } else {
                logger.v(tag, it)
            }
        }
    }

    fun w(
        tag: String? = null,
        msg: String,
        buildType: String = BuildConfig.BUILD_TYPE,
        cb: () -> String = { msg }
    ) {
        cb().chunked(900).forEach {
            if (buildType.equals("release")) {
                logger.printLine(tag, logger.warnTag, it)
            } else {
                logger.w(tag, it)
            }
        }
    }
}
