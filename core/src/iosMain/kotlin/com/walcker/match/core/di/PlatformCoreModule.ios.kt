package com.walcker.match.core.di

import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.IosAnalyticsTracker
import com.walcker.match.core.analytics.IosCrashReporter
import com.walcker.match.core.location.AddressGeocoder
import com.walcker.match.core.location.LocationProvider
import com.walcker.match.core.location.ReverseGeocoder
import com.walcker.match.core.location.createAddressGeocoder
import com.walcker.match.core.location.createLocationProvider
import com.walcker.match.core.location.createReverseGeocoder
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformCoreModule: Module =
    module {
        single<AnalyticsTracker> { IosAnalyticsTracker() }
        single<CrashReporter> { IosCrashReporter() }
        single<LocationProvider> { createLocationProvider() }
        single<ReverseGeocoder> { createReverseGeocoder() }
        single<AddressGeocoder> { createAddressGeocoder() }
    }
