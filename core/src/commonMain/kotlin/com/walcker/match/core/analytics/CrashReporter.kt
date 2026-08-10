package com.walcker.match.core.analytics

public interface CrashReporter {
    fun setKey(key: String, value: String)
    fun recordException(throwable: Throwable)
    fun log(message: String)
    fun forceCrash()
}
