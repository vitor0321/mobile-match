package com.walcker.games.features.ui.gamelist

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.usecase.JoinGameUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class GameListStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
    private val joinGame: JoinGameUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
) : ScreenModel {

    init {
        // Topo do funil. No init, e não a cada emissão da lista: filtro e raio
        // reemitem o tempo todo, e contar isso como visualização nova encheria
        // o topo de movimento que ninguém fez.
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.HOME))
    }

    private val strings get() = stringsHolder.resolveStringsOrDefault().gameList

    private val _state = MutableStateFlow(GameListState())
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
            is GameListEvents.JoinGame -> onJoinGame(event.gameId)
            is GameListEvents.SelectSport -> {
                screenModelScope.launch { preferences.setSelectedSport(event.sport) }
            }
            is GameListEvents.SetRadius -> {
                screenModelScope.launch { preferences.setRadiusKm(event.radiusKm) }
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
            val filtered = games.filter { game ->
                sport == null || game.sport == sport
            }.sortedBy { (it.lat * it.lat + it.lng * it.lng) } // TODO: sort by distance from user
            _state.update {
                it.copy(
                    selectedSport = sport,
                    radiusKm = radius,
                    games = filtered.toImmutableList(),
                    preferencesLoaded = true,
                    isLoading = false,
                )
            }
        }.launchIn(screenModelScope)
    }

    private fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refresh()
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: strings.loadErrorMessage,
                        )
                    }
                }
        }
    }

    private fun onJoinGame(gameId: String) {
        screenModelScope.launch {
            joinGame(gameId)
                .onSuccess {
                    _effects.send(GameListEffect.ShowMessage(strings.joinSuccess))
                    refresh()
                }
                .onFailure {
                    _effects.send(GameListEffect.ShowMessage(strings.joinError))
                }
        }
    }
}
