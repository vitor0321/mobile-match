package com.walcker.games.features.ui.gamelist

import com.walcker.games.features.domain.model.Game
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class GameListState(
    val isLoading: Boolean = true,
    val games: ImmutableList<Game> = persistentListOf(),
    val errorMessage: String? = null,
)
