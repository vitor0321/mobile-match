package com.walcker.games.features.data.di

import com.walcker.games.features.data.cache.InMemoryMatchCache
import com.walcker.games.features.data.platform.GamesPlatformServices
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.data.repository.GameRepositoryImpl
import com.walcker.games.features.data.source.FirestoreGameSource
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCase
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCaseImpl
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCaseImpl
import com.walcker.match.firestore.FirestoreClient
import org.koin.dsl.module

internal val gamesDataModule = module {
    single<GameSource> { FirestoreGameSource(firestore = get<FirestoreClient>()) }
    single<GamesPreferences> {
        GamesPreferences(dataStore = get<GamesPlatformServices>().gamesPreferencesDataStore())
    }
    single<InMemoryMatchCache> { InMemoryMatchCache() }
    single<GameRepository> {
        GameRepositoryImpl(
            source = get(),
            cache = get(),
        )
    }
    factory<GetOpenGamesUseCase> { GetOpenGamesUseCaseImpl(repository = get()) }
    factory<JoinGameUseCase> { JoinGameUseCaseImpl(repository = get()) }
}
