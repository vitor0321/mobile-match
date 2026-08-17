package com.walcker.games.features.ui.di

import com.walcker.games.features.ui.creatematch.CreateMatchStepModel
import com.walcker.games.features.ui.gamelist.GameListStepModel
import com.walcker.games.features.ui.map.MapStepModel
import com.walcker.games.features.ui.mymatches.MyMatchesStepModel
import com.walcker.games.features.ui.notifications.NotificationHistoryStepModel
import com.walcker.games.features.ui.playerprofile.PlayerProfileStepModel
import com.walcker.games.features.ui.search.SearchStepModel
import org.koin.dsl.module

internal val gamesUiModule = module {
    factory {
        GameListStepModel(
            repository = get(),
            preferences = get(),
            joinGame = get(),
            stringsHolder = get(),
        )
    }
    factory {
        MapStepModel(
            repository = get(),
            preferences = get(),
        )
    }
    factory {
        SearchStepModel(
            repository = get(),
            joinGame = get(),
            stringsHolder = get(),
        )
    }
    factory {
        CreateMatchStepModel(
            createMatch = get(),
            stringsHolder = get(),
            sessionHolder = get(),
            tabCoordinator = get(),
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
            stringsHolder = get(),
            logoutService = get(),
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
}
