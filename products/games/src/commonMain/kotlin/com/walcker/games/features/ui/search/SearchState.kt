package com.walcker.games.features.ui.search

import com.walcker.games.features.domain.model.Game
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SearchState(
    val query: String = "",
    val results: ImmutableList<Game> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
