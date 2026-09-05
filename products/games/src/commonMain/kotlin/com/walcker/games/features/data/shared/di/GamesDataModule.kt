package com.walcker.games.features.data.shared.di

import com.walcker.games.features.data.home.preferences.GamesPreferences
import com.walcker.games.features.data.shared.cache.InMemoryMatchCache
import com.walcker.games.features.data.shared.cache.InMemoryPlayerCache
import com.walcker.games.features.data.shared.platform.GamesPlatformServices
import com.walcker.games.features.data.shared.platform.createPlayerSource
import com.walcker.games.features.data.shared.repository.AvailabilityRepositoryImpl
import com.walcker.games.features.data.shared.repository.GameRepositoryImpl
import com.walcker.games.features.data.shared.repository.NotificationRepositoryImpl
import com.walcker.games.features.data.shared.repository.PlayerRepositoryImpl
import com.walcker.games.features.data.shared.repository.RatingRepositoryImpl
import com.walcker.games.features.data.shared.repository.ReportRepositoryImpl
import com.walcker.games.features.data.shared.source.AvailabilitySource
import com.walcker.games.features.data.shared.source.FirestoreAvailabilitySource
import com.walcker.games.features.data.shared.source.FirestoreGameSource
import com.walcker.games.features.data.shared.source.FirestoreNotificationSource
import com.walcker.games.features.data.shared.source.FirestoreRatingSource
import com.walcker.games.features.data.shared.source.FirestoreReportSource
import com.walcker.games.features.data.shared.source.GameSource
import com.walcker.games.features.data.shared.source.NotificationSource
import com.walcker.games.features.data.shared.source.PlayerSource
import com.walcker.games.features.data.shared.source.RatingSource
import com.walcker.games.features.data.shared.source.ReportSource
import com.walcker.games.features.domain.create.usecase.CreateMatchUseCase
import com.walcker.games.features.domain.create.usecase.CreateMatchUseCaseImpl
import com.walcker.games.features.domain.create.usecase.UpdateMatchUseCase
import com.walcker.games.features.domain.create.usecase.UpdateMatchUseCaseImpl
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCase
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.playerProfile.usecase.SetAvailabilityUseCase
import com.walcker.games.features.domain.playerProfile.usecase.SetAvailabilityUseCaseImpl
import com.walcker.games.features.domain.playerProfile.usecase.SetAvailableSportsUseCase
import com.walcker.games.features.domain.playerProfile.usecase.SetAvailableSportsUseCaseImpl
import com.walcker.games.features.domain.shared.repository.AvailabilityRepository
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.domain.shared.repository.NotificationRepository
import com.walcker.games.features.domain.shared.repository.PlayerRepository
import com.walcker.games.features.domain.shared.repository.RatingRepository
import com.walcker.games.features.domain.shared.repository.ReportRepository
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCase
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.DeleteNotificationUseCase
import com.walcker.games.features.domain.shared.usecase.DeleteNotificationUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetMyMatchesUseCase
import com.walcker.games.features.domain.shared.usecase.GetMyMatchesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetNotificationHistoryUseCase
import com.walcker.games.features.domain.shared.usecase.GetNotificationHistoryUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetPlayerDetailsUseCase
import com.walcker.games.features.domain.shared.usecase.GetPlayerDetailsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetPlayerRatingsUseCase
import com.walcker.games.features.domain.shared.usecase.GetPlayerRatingsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetUserRatingsUseCase
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCase
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.MarkNotificationAsReadUseCase
import com.walcker.games.features.domain.shared.usecase.MarkNotificationAsReadUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SearchPlayersUseCase
import com.walcker.games.features.domain.shared.usecase.SearchPlayersUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SubmitMatchRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCaseImpl
import com.walcker.match.firestore.FirestoreClient
import org.koin.dsl.module

internal val gamesDataModule =
    module {
        single<GameSource> {
            FirestoreGameSource(
                firestore = get<FirestoreClient>(),
                sessionHolder = get(),
                locationProvider = get(),
            )
        }
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
        factory<JoinGameUseCase> { JoinGameUseCaseImpl(repository = get()) }
        factory<CreateMatchUseCase> { CreateMatchUseCaseImpl(repository = get()) }
        factory<UpdateMatchUseCase> { UpdateMatchUseCaseImpl(repository = get()) }
        factory<GetMyMatchesUseCase> { GetMyMatchesUseCaseImpl(repository = get()) }
        factory<CancelMatchUseCase> { CancelMatchUseCaseImpl(repository = get()) }
        factory<CancelMatchSeriesUseCase> { CancelMatchSeriesUseCaseImpl(repository = get()) }
        factory<LeaveMatchUseCase> { LeaveMatchUseCaseImpl(repository = get()) }
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
        factory { SubmitMatchRatingUseCase(ratingRepository = get()) }
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
        factory<SetAvailableSportsUseCase> { SetAvailableSportsUseCaseImpl(repository = get()) }
    }
