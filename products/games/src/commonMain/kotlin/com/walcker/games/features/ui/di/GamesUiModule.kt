package com.walcker.games.features.ui.di

import com.walcker.games.features.ui.create.CreateMatchStepModel
import com.walcker.games.features.ui.create.locationPicker.LocationPickerStepModel
import com.walcker.games.features.ui.home.GameListStepModel
import com.walcker.games.features.ui.home.map.MapStepModel
import com.walcker.games.features.ui.myMatches.MyMatchesStepModel
import com.walcker.games.features.ui.playerProfile.PlayerProfileStepModel
import com.walcker.games.features.ui.search.SearchStepModel
import com.walcker.games.features.ui.shared.notifications.NotificationHistoryStepModel
import com.walcker.games.features.ui.shared.playerDetails.PlayerDetailsStepModel
import com.walcker.games.features.ui.shared.playerRatings.PlayerRatingsListStepModel
import com.walcker.games.features.ui.shared.playerSearch.PlayerSearchStepModel
import org.koin.dsl.module

internal val gamesUiModule =
    module {
        factory {
            GameListStepModel(
                repository = get(),
                preferences = get(),
                stringsHolder = get(),
                analytics = get(),
                homeViewCoordinator = get(),
            )
        }
        factory {
            MapStepModel(
                repository = get(),
                preferences = get(),
                locationProvider = get(),
                analytics = get(),
            )
        }
        factory {
            SearchStepModel(
                repository = get(),
                stringsHolder = get(),
                analytics = get(),
            )
        }
        factory { (matchId: String?) ->
            CreateMatchStepModel(
                createMatch = get(),
                updateMatch = get(),
                getGameById = get(),
                stringsHolder = get(),
                sessionHolder = get(),
                tabCoordinator = get(),
                analytics = get(),
                locationProvider = get(),
                reverseGeocoder = get(),
                editingMatchId = matchId,
            )
        }
        factory { (initialLat: Double, initialLng: Double) ->
            LocationPickerStepModel(
                initialLat = initialLat,
                initialLng = initialLng,
                reverseGeocoder = get(),
                addressGeocoder = get(),
            )
        }
        factory {
            MyMatchesStepModel(
                getMyMatches = get(),
                cancelMatch = get(),
                leaveMatch = get(),
                stringsHolder = get(),
                sessionHolder = get(),
            )
        }
        factory {
            PlayerProfileStepModel(
                sessionHolder = get(),
                getMyMatches = get(),
                getUserRatings = get(),
                stringsHolder = get(),
                logoutService = get(),
                observeAvailability = get(),
                setAvailability = get(),
                setAvailableSports = get(),
            )
        }
        factory {
            NotificationHistoryStepModel(
                getNotificationHistory = get(),
                markNotificationAsRead = get(),
                deleteNotification = get(),
                sessionHolder = get(),
                stringsHolder = get(),
            )
        }
        factory {
            PlayerSearchStepModel(
                searchPlayersUseCase = get(),
                stringsHolder = get(),
            )
        }
        factory { (userId: String) ->
            PlayerDetailsStepModel(
                userId = userId,
                getPlayerDetails = get(),
                getPlayerRatings = get(),
                stringsHolder = get(),
            )
        }
        factory { (userId: String, playerName: String) ->
            PlayerRatingsListStepModel(
                userId = userId,
                playerName = playerName,
                getPlayerRatings = get(),
                stringsHolder = get(),
            )
        }
    }
