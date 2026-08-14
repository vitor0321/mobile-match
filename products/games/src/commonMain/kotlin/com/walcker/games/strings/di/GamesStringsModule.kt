package com.walcker.games.strings.di

import com.walcker.games.strings.GamesStringsHolder
import org.koin.dsl.module

internal val gamesStringsModule = module {
    single { GamesStringsHolder() }
}
