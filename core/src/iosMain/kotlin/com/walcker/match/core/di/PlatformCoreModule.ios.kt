package com.walcker.match.core.di

import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.IosAnalyticsTracker
import com.walcker.match.core.analytics.IosCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformCoreModule: Module = module {
    single<AnalyticsTracker> { IosAnalyticsTracker() }
    single<CrashReporter> { IosCrashReporter() }
}
