package com.walcker.games.di

import androidx.datastore.core.DataStore
import com.walcker.identity.api.AccountDeletionService
import com.walcker.identity.api.LogoutService
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.location.AddressGeocoder
import com.walcker.match.core.location.LocationProvider
import com.walcker.match.core.location.ReverseGeocoder
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.test.Test

@OptIn(KoinExperimentalAPI::class)
class GamesModuleTest {
    private val providedByOtherModules =
        listOf(
            DataStore::class,
            AnalyticsTracker::class,
            CrashReporter::class,
            LocationProvider::class,
            ReverseGeocoder::class,
            AddressGeocoder::class,
            SessionHolder::class,
            LogoutService::class,
            AccountDeletionService::class,
        )

    @Test
    fun `every definition finds its dependencies`() {
        gamesModule.verify(extraTypes = providedByOtherModules)
    }
}
