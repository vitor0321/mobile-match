package com.walcker.games.features.ui.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
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
    private val joinGame: JoinGameUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().search

    private val _state = MutableStateFlow(SearchState())
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
                applyQuery(event.query)
            }
            is SearchEvents.JoinGame -> onJoinGame(event.gameId)
        }
    }

    private fun applyQuery(query: String) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isBlank()) {
            allMatches
        } else {
            val needle = trimmed.lowercase()
            allMatches.filter { game ->
                game.venueName.lowercase().contains(needle) ||
                    game.neighborhood.lowercase().contains(needle) ||
                    game.city.lowercase().contains(needle) ||
                    game.sport.label.lowercase().contains(needle)
            }
        }
        _state.update {
            it.copy(results = filtered.toImmutableList())
        }
    }

    private fun onJoinGame(gameId: String) {
        screenModelScope.launch {
            joinGame(gameId)
                .onSuccess {
                    _effects.send(SearchEffect.ShowMessage(strings.joinSuccess))
                }
                .onFailure {
                    _effects.send(SearchEffect.ShowMessage(strings.joinError))
                }
        }
    }
}
