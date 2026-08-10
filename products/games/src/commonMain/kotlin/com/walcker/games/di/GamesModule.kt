package com.walcker.games.di

import com.walcker.games.GamesDestinationImpl
import com.walcker.games.features.data.di.gamesDataModule
import com.walcker.games.features.ui.di.gamesUiModule
import com.walcker.match.navigator.GamesDestination
import org.koin.dsl.module

val gamesModule = module {
    includes(gamesDataModule, gamesUiModule)
    single<GamesDestination> { GamesDestinationImpl() }
}
