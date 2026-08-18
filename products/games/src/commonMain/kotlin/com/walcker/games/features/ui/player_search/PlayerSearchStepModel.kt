package com.walcker.games.features.ui.player_search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.usecase.SearchPlayersUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ScreenModel for player search functionality.
 *
 * - Manages search query and filters
 * - Calls SearchPlayersUseCase with current filters
 * - Handles navigation to player details
 */
internal class PlayerSearchStepModel(
    private val searchPlayersUseCase: SearchPlayersUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().playerSearch

    private val _state = MutableStateFlow(PlayerSearchState())
    val state: StateFlow<PlayerSearchState> = _state.asStateFlow()

    private val _effects = Channel<PlayerSearchEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerSearchEffect> = _effects.receiveAsFlow()

    fun onEvent(event: PlayerSearchEvents) {
        when (event) {
            is PlayerSearchEvents.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                performSearch()
            }
            is PlayerSearchEvents.FiltersChanged -> {
                _state.update { it.copy(filters = event.filters) }
                performSearch()
            }
            is PlayerSearchEvents.MinRatingChanged -> {
                _state.update {
                    it.copy(filters = it.filters.copy(minRating = event.minRating))
                }
                performSearch()
            }
            is PlayerSearchEvents.MaxRatingChanged -> {
                _state.update {
                    it.copy(filters = it.filters.copy(maxRating = event.maxRating))
                }
                performSearch()
            }
            is PlayerSearchEvents.SportsFilterChanged -> {
                _state.update {
                    it.copy(filters = it.filters.copy(favoriteSports = event.sports))
                }
                performSearch()
            }
            PlayerSearchEvents.ResetFilters -> {
                _state.update {
                    it.copy(
                        query = "",
                        filters = PlayerSearchFilters(),
                        showFiltersPanel = false,
                    )
                }
                performSearch()
            }
            PlayerSearchEvents.ToggleFiltersPanel -> {
                _state.update { it.copy(showFiltersPanel = !it.showFiltersPanel) }
            }
            is PlayerSearchEvents.SelectPlayer -> {
                screenModelScope.launch {
                    _effects.send(PlayerSearchEffect.NavigateToPlayer(event.userId))
                }
            }
        }
    }

    private fun performSearch() {
        screenModelScope.launch {
            val currentState = _state.value

            // Only search if query is not blank or filters are active
            if (currentState.query.isBlank() &&
                currentState.filters == PlayerSearchFilters()
            ) {
                _state.update { it.copy(results = persistentListOf()) }
                return@launch
            }

            _state.update { it.copy(isLoading = true, errorMessage = null) }

            searchPlayersUseCase(currentState.filters)
                .onSuccess { results ->
                    // Apply client-side query filter
                    val filtered = if (currentState.query.isBlank()) {
                        results
                    } else {
                        val trimmedQuery = currentState.query.trim().lowercase()
                        results.filter { player ->
                            player.displayName.lowercase().contains(trimmedQuery)
                        }
                    }
                    _state.update {
                        it.copy(
                            results = filtered.toImmutableList(),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: strings.errorLoading
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message,
                        )
                    }
                    _effects.send(PlayerSearchEffect.ShowMessage(message))
                }
        }
    }
}
