package com.walcker.match.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.walcker.identity.features.data.billing.PurchasesBootstrap
import com.walcker.match.app.di.initKoin
import org.koin.android.ext.koin.androidContext

internal class MatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PurchasesBootstrap.configure(BuildConfig.REVENUECAT_ANDROID_KEY)
        initKoin(isDebug = BuildConfig.IS_DEBUG) {
            androidContext(this@MatchApplication)
        }
        MobileAds.initialize(this)
        if (BuildConfig.IS_DEBUG) {
            Log.d("Join Play", "Running in debug mode — build: ${BuildConfig.VERSION_NAME}")
        }
    }
}
