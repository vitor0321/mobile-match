package com.walcker.games.di

import com.walcker.games.GamesDestinationImpl
import com.walcker.games.features.data.di.gamesDataModule
import com.walcker.games.features.data.platform.gamesPlatformModule
import com.walcker.games.features.ui.di.gamesUiModule
import com.walcker.games.strings.di.gamesStringsModule
import com.walcker.match.navigator.DeepLinkCoordinator
import com.walcker.match.navigator.GamesDestination
import com.walcker.match.navigator.TabCoordinator
import org.koin.dsl.module

val gamesModule = module {
    includes(gamesDataModule, gamesUiModule, gamesPlatformModule, gamesStringsModule)
    single<GamesDestination> { GamesDestinationImpl() }
    single { TabCoordinator() }
    single { DeepLinkCoordinator() }
}
