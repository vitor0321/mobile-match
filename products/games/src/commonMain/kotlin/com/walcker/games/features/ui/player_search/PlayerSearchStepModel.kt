package com.walcker.games.features.ui.player_search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.usecase.SearchPlayersUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlayerSearchStepModel(
    private val searchPlayersUseCase: SearchPlayersUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val debounceMs: Long = SEARCH_DEBOUNCE_MS,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().playerSearch

    private val _state = MutableStateFlow(PlayerSearchState())
    val state: StateFlow<PlayerSearchState> = _state.asStateFlow()

    private val _effects = Channel<PlayerSearchEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerSearchEffect> = _effects.receiveAsFlow()

    private var searchJob: Job? = null

    fun onEvent(event: PlayerSearchEvents) {
        when (event) {
            is PlayerSearchEvents.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                scheduleSearch()
            }

            is PlayerSearchEvents.FiltersChanged -> {
                _state.update { it.copy(filters = event.filters) }
                scheduleSearch()
            }

            is PlayerSearchEvents.MinRatingChanged -> {
                _state.update { it.copy(filters = it.filters.copy(minRating = event.minRating)) }
                scheduleSearch()
            }

            is PlayerSearchEvents.MaxRatingChanged -> {
                _state.update { it.copy(filters = it.filters.copy(maxRating = event.maxRating)) }
                scheduleSearch()
            }

            is PlayerSearchEvents.SportsFilterChanged -> {
                _state.update { it.copy(filters = it.filters.copy(favoriteSports = event.sports)) }
                scheduleSearch()
            }

            PlayerSearchEvents.ResetFilters -> {
                _state.update {
                    it.copy(
                        query = "",
                        filters = PlayerSearchFilters(),
                        showFiltersPanel = false,
                    )
                }
                scheduleSearch(debounce = false)
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

    private fun scheduleSearch(debounce: Boolean = true) {
        searchJob?.cancel()

        val current = _state.value
        if (current.isIdle) {
            _state.update {
                it.copy(
                    results = persistentListOf(),
                    isLoading = false,
                    reachedLimit = false,
                    errorMessage = null,
                )
            }
            return
        }

        searchJob = screenModelScope.launch {
            if (debounce) delay(debounceMs)

            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val filters = current.filters.copy(query = current.query.trim())

            searchPlayersUseCase(filters)
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            results = results.players.toImmutableList(),
                            reachedLimit = results.reachedLimit,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: strings.errorLoading
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                    _effects.send(PlayerSearchEffect.ShowMessage(message))
                }
        }
    }

    internal companion object {
        internal const val SEARCH_DEBOUNCE_MS: Long = 300L
    }
}
