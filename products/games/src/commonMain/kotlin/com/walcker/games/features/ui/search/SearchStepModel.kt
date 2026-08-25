package com.walcker.games.features.ui.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.MatchListSource
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SearchStepModel(
    private val repository: GameRepository,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
) : ScreenModel {

    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.SEARCH))
    }

    private val gamesStrings get() = stringsHolder.resolveStringsOrDefault()

    private val _state = MutableStateFlow(
        SearchState(
            strings = gamesStrings.search,
            cardStrings = gamesStrings.gameList,
        ),
    )
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private var allMatches: List<com.walcker.games.features.domain.model.Game> = emptyList()

    init {
        // Observe cache and re-apply the current query
        repository.observeMatches()
            .onEach { games ->
                allMatches = games
                applyQuery(_state.value.query)
            }
            .launchIn(screenModelScope)
    }

    fun onEvent(event: SearchEvents) {
        when (event) {
            is SearchEvents.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                applyFilters()
            }
            is SearchEvents.DateRangeChanged -> {
                _state.update { it.copy(filters = it.filters.copy(startDateMs = event.startDateMs, endDateMs = event.endDateMs)) }
                applyFilters()
            }
            is SearchEvents.SportFilterChanged -> {
                _state.update { it.copy(filters = it.filters.copy(sports = event.sports)) }
                applyFilters()
            }
            is SearchEvents.PriceRangeChanged -> {
                _state.update { it.copy(filters = it.filters.copy(minPrice = event.minPrice, maxPrice = event.maxPrice)) }
                applyFilters()
            }
            SearchEvents.ResetFilters -> {
                _state.update { it.copy(query = "", filters = SearchFilters(), showFiltersPanel = false) }
                applyFilters()
            }
            SearchEvents.ToggleFiltersPanel -> {
                _state.update { it.copy(showFiltersPanel = !it.showFiltersPanel) }
            }
            is SearchEvents.SelectGame -> {
                screenModelScope.launch {
                    _effects.send(SearchEffect.NavigateToMatchDetail(event.gameId))
                }
            }
        }
    }

    private fun applyFilters() {
        val state = _state.value
        val trimmedQuery = state.query.trim().lowercase()
        val filters = state.filters

        val filtered = allMatches.filter { game ->
            // Text search
            val matchesText = if (trimmedQuery.isBlank()) {
                true
            } else {
                game.venueName.lowercase().contains(trimmedQuery) ||
                    game.neighborhood.lowercase().contains(trimmedQuery) ||
                    game.city.lowercase().contains(trimmedQuery) ||
                    game.sport.label.lowercase().contains(trimmedQuery)
            }

            // Date range filter (convert seconds to milliseconds for comparison)
            val gameStartMs = game.startsAtSeconds * 1000
            val matchesDateRange = if (filters.startDateMs != null && gameStartMs < filters.startDateMs) {
                false
            } else if (filters.endDateMs != null && gameStartMs > filters.endDateMs) {
                false
            } else {
                true
            }

            // Sport filter
            val matchesSport = if (filters.sports.isEmpty()) {
                true
            } else {
                game.sport in filters.sports
            }

            // Price filter (pricePerPlayer can be null for free matches)
            val gamePrice = game.pricePerPlayer?.toFloatOrNull() ?: 0f
            val matchesPrice = if (filters.minPrice != null && gamePrice < filters.minPrice) {
                false
            } else if (filters.maxPrice != null && gamePrice > filters.maxPrice) {
                false
            } else {
                true
            }

            matchesText && matchesDateRange && matchesSport && matchesPrice
        }

        _state.update {
            // Re-carimba as strings: o holder só é preenchido depois da primeira
            // composição, então o valor inicial pode ter caído no padrão pt-BR.
            it.copy(
                strings = gamesStrings.search,
                cardStrings = gamesStrings.gameList,
                results = filtered.toImmutableList(),
            )
        }
    }

    private fun applyQuery(query: String) {
        _state.update { it.copy(query = query) }
        applyFilters()
    }
}
