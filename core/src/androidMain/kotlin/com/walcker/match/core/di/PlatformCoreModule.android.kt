package com.walcker.match.core.di

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.FirebaseAnalyticsTracker
import com.walcker.match.core.analytics.FirebaseCrashReporter
import com.walcker.match.core.location.AddressGeocoder
import com.walcker.match.core.location.LocationProvider
import com.walcker.match.core.location.ReverseGeocoder
import com.walcker.match.core.location.createAddressGeocoder
import com.walcker.match.core.location.createLocationProvider
import com.walcker.match.core.location.createReverseGeocoder
import com.walcker.match.core.navigation.CurrentActivityHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformCoreModule: Module =
    module {
        single<AnalyticsTracker> {
            FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(androidContext()))
        }
        single<CrashReporter>(createdAtStart = true) { FirebaseCrashReporter() }
        single(createdAtStart = true) {
            val app = androidContext() as Application
            CurrentActivityHolder().also { holder ->
                holder.setApplication(app)
                app.registerActivityLifecycleCallbacks(holder)
            }
        }
        single<LocationProvider> { createLocationProvider() }
        single<ReverseGeocoder> { createReverseGeocoder() }
        single<AddressGeocoder> { createAddressGeocoder() }
    }
