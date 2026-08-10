package com.walcker.games.features.data.di

import com.walcker.games.features.data.repository.GameRepositoryImpl
import com.walcker.games.features.data.source.GameSource
import com.walcker.games.features.data.source.InMemoryGameSource
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCase
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCaseImpl
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCaseImpl
import org.koin.dsl.module

internal val gamesDataModule = module {
    single<GameSource> { InMemoryGameSource() }
    single<GameRepository> { GameRepositoryImpl(source = get()) }
    factory<GetOpenGamesUseCase> { GetOpenGamesUseCaseImpl(repository = get()) }
    factory<JoinGameUseCase> { JoinGameUseCaseImpl(repository = get()) }
}
