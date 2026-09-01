package com.walcker.match.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseCrashReporter : CrashReporter {
    private val crashlytics =
        FirebaseCrashlytics.getInstance().also {
            it.setCrashlyticsCollectionEnabled(true)
        }

    override fun setKey(
        key: String,
        value: String,
    ) {
        crashlytics.setCustomKey(key, value)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun forceCrash(): Unit = throw RuntimeException("Test crash from Crashlytics")
}
