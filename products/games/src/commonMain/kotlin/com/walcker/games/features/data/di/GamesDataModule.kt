package com.walcker.games.features.data.di

import com.walcker.games.features.data.cache.InMemoryMatchCache
import com.walcker.games.features.data.platform.GamesPlatformServices
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.data.repository.GameRepositoryImpl
import com.walcker.games.features.data.repository.NotificationRepositoryImpl
import com.walcker.games.features.data.source.FirestoreGameSource
import com.walcker.games.features.data.source.FirestoreNotificationSource
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.data.source.NotificationSource
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.repository.NotificationRepository
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.CreateMatchUseCase
import com.walcker.games.features.domain.usecase.CreateMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCase
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCaseImpl
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCase
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCaseImpl
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCaseImpl
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.UpdatePushTokenUseCase
import com.walcker.games.features.domain.usecase.UpdatePushTokenUseCaseImpl
import com.walcker.games.features.domain.usecase.GetNotificationHistoryUseCase
import com.walcker.games.features.domain.usecase.GetNotificationHistoryUseCaseImpl
import com.walcker.match.firestore.FirestoreClient
import org.koin.dsl.module

internal val gamesDataModule = module {
    single<GameSource> { FirestoreGameSource(firestore = get<FirestoreClient>(), sessionHolder = get()) }
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
    single<NotificationSource> { FirestoreNotificationSource(firestore = get<FirestoreClient>()) }
    single<NotificationRepository> {
        NotificationRepositoryImpl(source = get())
    }
    factory<GetOpenGamesUseCase> { GetOpenGamesUseCaseImpl(repository = get()) }
    factory<JoinGameUseCase> { JoinGameUseCaseImpl(repository = get()) }
    factory<CreateMatchUseCase> { CreateMatchUseCaseImpl(repository = get()) }
    factory<GetMyMatchesUseCase> { GetMyMatchesUseCaseImpl(repository = get()) }
    factory<CancelMatchUseCase> { CancelMatchUseCaseImpl(repository = get()) }
    factory<LeaveMatchUseCase> { LeaveMatchUseCaseImpl(repository = get()) }
    factory<UpdatePushTokenUseCase> { UpdatePushTokenUseCaseImpl(notificationRepository = get()) }
    factory<GetNotificationHistoryUseCase> { GetNotificationHistoryUseCaseImpl(repository = get()) }
}
