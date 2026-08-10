package com.walcker.match.core.analytics

internal class IosCrashReporter : CrashReporter {
    override fun setKey(key: String, value: String) {}
    override fun recordException(throwable: Throwable) {}
    override fun log(message: String) {}
    override fun forceCrash(): Unit = throw RuntimeException("Test crash from Crashlytics")
}
