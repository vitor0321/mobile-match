package com.walcker.games.features.ui.di

import com.walcker.games.features.ui.gamelist.GameListStepModel
import org.koin.dsl.module

internal val gamesUiModule = module {
    factory {
        GameListStepModel(
            getOpenGames = get(),
            joinGame = get(),
        )
    }
}
