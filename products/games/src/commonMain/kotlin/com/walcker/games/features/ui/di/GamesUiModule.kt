package com.walcker.games.features.ui.di

import com.walcker.games.features.ui.gamelist.GameListStepModel
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
        SearchStepModel(
            repository = get(),
            joinGame = get(),
            stringsHolder = get(),
        )
    }
}
