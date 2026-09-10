package com.walcker.match.core.analytics

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

private const val CRASHLYTICS_ERROR_DOMAIN = "KotlinException"

@OptIn(ExperimentalForeignApi::class)
internal class IosCrashReporter : CrashReporter {
    private val crashlytics = FIRCrashlytics.crashlytics()

    override fun setKey(
        key: String,
        value: String,
    ) {
        crashlytics.setCustomValue(value, forKey = key)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.log(throwable.stackTraceToString())
        crashlytics.recordError(
            NSError(
                domain = CRASHLYTICS_ERROR_DOMAIN,
                code = 0,
                userInfo =
                    mapOf(
                        NSLocalizedDescriptionKey to (throwable.message ?: throwable::class.simpleName ?: "Unknown"),
                    ),
            ),
        )
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun forceCrash(): Unit = throw RuntimeException("Test crash from Crashlytics")
}
