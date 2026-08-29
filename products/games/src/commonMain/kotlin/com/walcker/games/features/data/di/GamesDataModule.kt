package com.walcker.games.features.data.di

import com.walcker.games.features.data.cache.InMemoryMatchCache
import com.walcker.games.features.data.cache.InMemoryPlayerCache
import com.walcker.games.features.data.platform.GamesPlatformServices
import com.walcker.games.features.data.platform.createPlayerSource
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.data.repository.AvailabilityRepositoryImpl
import com.walcker.games.features.data.repository.GameRepositoryImpl
import com.walcker.games.features.data.repository.NotificationRepositoryImpl
import com.walcker.games.features.data.repository.PlayerRepositoryImpl
import com.walcker.games.features.data.repository.RatingRepositoryImpl
import com.walcker.games.features.data.repository.ReportRepositoryImpl
import com.walcker.games.features.data.source.AvailabilitySource
import com.walcker.games.features.data.source.FirestoreAvailabilitySource
import com.walcker.games.features.data.source.FirestoreGameSource
import com.walcker.games.features.data.source.FirestoreNotificationSource
import com.walcker.games.features.data.source.FirestoreRatingSource
import com.walcker.games.features.data.source.FirestoreReportSource
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.data.source.NotificationSource
import com.walcker.games.features.data.source.PlayerSource
import com.walcker.games.features.data.source.RatingSource
import com.walcker.games.features.data.source.ReportSource
import com.walcker.games.features.domain.repository.AvailabilityRepository
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.repository.NotificationRepository
import com.walcker.games.features.domain.repository.PlayerRepository
import com.walcker.games.features.domain.repository.RatingRepository
import com.walcker.games.features.domain.repository.ReportRepository
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.CreateMatchUseCase
import com.walcker.games.features.domain.usecase.CreateMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.DeleteNotificationUseCase
import com.walcker.games.features.domain.usecase.DeleteNotificationUseCaseImpl
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCase
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCaseImpl
import com.walcker.games.features.domain.usecase.GetNotificationHistoryUseCase
import com.walcker.games.features.domain.usecase.GetNotificationHistoryUseCaseImpl
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCase
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCaseImpl
import com.walcker.games.features.domain.usecase.GetPlayerDetailsUseCase
import com.walcker.games.features.domain.usecase.GetPlayerDetailsUseCaseImpl
import com.walcker.games.features.domain.usecase.GetPlayerRatingsUseCase
import com.walcker.games.features.domain.usecase.GetPlayerRatingsUseCaseImpl
import com.walcker.games.features.domain.usecase.GetUserRatingsUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCaseImpl
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.MarkNotificationAsReadUseCase
import com.walcker.games.features.domain.usecase.MarkNotificationAsReadUseCaseImpl
import com.walcker.games.features.domain.usecase.ObserveAvailabilityUseCase
import com.walcker.games.features.domain.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.usecase.SearchPlayersUseCase
import com.walcker.games.features.domain.usecase.SearchPlayersUseCaseImpl
import com.walcker.games.features.domain.usecase.SetAvailabilityUseCase
import com.walcker.games.features.domain.usecase.SetAvailabilityUseCaseImpl
import com.walcker.games.features.domain.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.usecase.SubmitReportUseCase
import com.walcker.games.features.domain.usecase.SubmitReportUseCaseImpl
import com.walcker.games.features.domain.usecase.UpdatePushTokenUseCase
import com.walcker.games.features.domain.usecase.UpdatePushTokenUseCaseImpl
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
    single<RatingSource> { FirestoreRatingSource(firestore = get<FirestoreClient>()) }
    single<RatingRepository> {
        RatingRepositoryImpl(ratingSource = get(), playerCache = get())
    }
    factory<GetOpenGamesUseCase> { GetOpenGamesUseCaseImpl(repository = get()) }
    factory<JoinGameUseCase> { JoinGameUseCaseImpl(repository = get()) }
    factory<CreateMatchUseCase> { CreateMatchUseCaseImpl(repository = get()) }
    factory<GetMyMatchesUseCase> { GetMyMatchesUseCaseImpl(repository = get()) }
    factory<CancelMatchUseCase> { CancelMatchUseCaseImpl(repository = get()) }
    factory<LeaveMatchUseCase> { LeaveMatchUseCaseImpl(repository = get()) }
    factory<UpdatePushTokenUseCase> { UpdatePushTokenUseCaseImpl(notificationRepository = get()) }
    factory<GetNotificationHistoryUseCase> { GetNotificationHistoryUseCaseImpl(repository = get()) }
    factory<GetGameByIdUseCase> { GetGameByIdUseCaseImpl(repository = get()) }
    factory<MarkNotificationAsReadUseCase> { MarkNotificationAsReadUseCaseImpl(repository = get()) }
    factory<DeleteNotificationUseCase> { DeleteNotificationUseCaseImpl(repository = get()) }
    factory<ObserveParticipantsUseCase> { ObserveParticipantsUseCaseImpl(repository = get()) }
    factory<ObserveMatchUseCase> { ObserveMatchUseCaseImpl(repository = get()) }
    single<ReportSource> { FirestoreReportSource(firestore = get<FirestoreClient>()) }
    single<ReportRepository> { ReportRepositoryImpl(source = get()) }
    factory<SubmitReportUseCase> { SubmitReportUseCaseImpl(repository = get()) }
    factory { SubmitRatingUseCase(ratingRepository = get()) }
    factory { GetUserRatingsUseCase(ratingRepository = get()) }

    single<PlayerSource> {
        createPlayerSource(
            firestore = get<FirestoreClient>(),
            ratingSource = get(),
        )
    }
    single<InMemoryPlayerCache> { InMemoryPlayerCache() }
    single<PlayerRepository> {
        PlayerRepositoryImpl(source = get(), cache = get())
    }
    factory<SearchPlayersUseCase> { SearchPlayersUseCaseImpl(repository = get()) }
    factory<GetPlayerDetailsUseCase> { GetPlayerDetailsUseCaseImpl(repository = get()) }
    factory<GetPlayerRatingsUseCase> { GetPlayerRatingsUseCaseImpl(repository = get()) }

    single<AvailabilitySource> { FirestoreAvailabilitySource(firestore = get<FirestoreClient>()) }
    single<AvailabilityRepository> { AvailabilityRepositoryImpl(source = get()) }
    factory<ObserveAvailabilityUseCase> { ObserveAvailabilityUseCaseImpl(repository = get()) }
    factory<SetAvailabilityUseCase> { SetAvailabilityUseCaseImpl(repository = get()) }
}
