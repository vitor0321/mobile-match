package com.walcker.games.features.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.home.preferences.GamesPreferences
import com.walcker.games.features.domain.shared.model.isDiscoverable
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.MatchListSource
import com.walcker.match.navigator.HomeViewCoordinator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1000L

internal class GameListStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val homeViewCoordinator: HomeViewCoordinator,
) : ScreenModel {
    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.HOME))
    }

    private val strings get() = stringsHolder.resolveStringsOrDefault().gameList

    private val _state = MutableStateFlow(GameListState(strings = strings))
    val state: StateFlow<GameListState> = _state.asStateFlow()

    private val _effects = Channel<GameListEffect>(Channel.BUFFERED)
    val effects: Flow<GameListEffect> = _effects.receiveAsFlow()

    init {
        observePreferencesAndMatches()
        refresh()
    }

    fun onEvent(event: GameListEvents) {
        when (event) {
            is GameListEvents.Refresh -> refresh()
            is GameListEvents.SelectSport -> {
                screenModelScope.launch { preferences.setSelectedSport(event.sport) }
            }
            is GameListEvents.SetRadius -> {
                screenModelScope.launch {
                    preferences.setRadiusKm(event.radiusKm)
                    refresh()
                }
            }
            is GameListEvents.SelectGame -> {
                screenModelScope.launch {
                    _effects.send(GameListEffect.NavigateToMatchDetail(event.gameId))
                }
            }
        }
    }

    private fun observePreferencesAndMatches() {
        combine(
            preferences.selectedSport,
            preferences.radiusKm,
            repository.observeMatches(),
        ) { sport, radius, games ->
            Triple(sport, radius, games)
        }.onEach { (sport, radius, games) ->
            val nowSeconds =
                kotlin.time.Clock.System
                    .now()
                    .toEpochMilliseconds() / MILLIS_PER_SECOND
            val filtered =
                games
                    .filter { game ->
                        (sport == null || game.sport == sport) && game.isDiscoverable(nowSeconds)
                    }.sortedBy { (it.lat * it.lat + it.lng * it.lng) } // TODO: sort by distance from user
            _state.update {
                it.copy(
                    strings = strings,
                    selectedSport = sport,
                    radiusKm = radius,
                    games = filtered.toImmutableList(),
                    preferencesLoaded = true,
                    isLoading = false,
                )
            }
            homeViewCoordinator.markHomeDataReady()
        }.launchIn(screenModelScope)
    }

    private fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val radiusKm = preferences.radiusKm.first()
            repository
                .refresh(radiusKm)
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: strings.loadErrorMessage,
                        )
                    }
                    homeViewCoordinator.markHomeDataReady()
                }
        }
    }
}
