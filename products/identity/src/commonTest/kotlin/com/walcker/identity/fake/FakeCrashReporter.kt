package com.walcker.identity.fake

import com.walcker.match.core.analytics.CrashReporter

internal class FakeCrashReporter : CrashReporter {
    val recordedExceptions: MutableList<Throwable> = mutableListOf()
    val loggedMessages: MutableList<String> = mutableListOf()
    val keys: MutableMap<String, String> = mutableMapOf()

    override fun setKey(
        key: String,
        value: String,
    ) {
        keys[key] = value
    }

    override fun recordException(throwable: Throwable) {
        recordedExceptions += throwable
    }

    override fun log(message: String) {
        loggedMessages += message
    }

    override fun forceCrash(): Unit = throw RuntimeException("Test crash from Crashlytics")
}
