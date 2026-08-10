package com.walcker.games.features.ui.gamelist

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.usecase.GetOpenGamesUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class GameListStepModel(
    private val getOpenGames: GetOpenGamesUseCase,
    private val joinGame: JoinGameUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(GameListState())
    val state: StateFlow<GameListState> = _state.asStateFlow()

    private val _effects = Channel<GameListEffect>(Channel.BUFFERED)
    val effects: Flow<GameListEffect> = _effects.receiveAsFlow()

    init {
        loadGames()
    }

    fun onEvent(event: GameListEvents) {
        when (event) {
            is GameListEvents.Refresh -> loadGames()
            is GameListEvents.JoinGame -> onJoinGame(event.gameId)
        }
    }

    private fun loadGames() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            getOpenGames()
                .onSuccess { games ->
                    _state.update {
                        it.copy(isLoading = false, games = games.toImmutableList())
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Não foi possível carregar as partidas.",
                        )
                    }
                }
        }
    }

    private fun onJoinGame(gameId: String) {
        screenModelScope.launch {
            joinGame(gameId)
                .onSuccess {
                    _effects.send(GameListEffect.ShowMessage("Você entrou no jogo."))
                    loadGames()
                }
                .onFailure {
                    _effects.send(GameListEffect.ShowMessage("Não foi possível entrar no jogo."))
                }
        }
    }
}
