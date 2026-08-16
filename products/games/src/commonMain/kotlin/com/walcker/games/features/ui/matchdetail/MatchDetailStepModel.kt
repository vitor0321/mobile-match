package com.walcker.games.features.ui.matchdetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for match detail screen.
 */
internal data class MatchDetailState(
    val match: Game? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Events for match detail screen.
 */
internal sealed interface MatchDetailEvent {
    data object Retry : MatchDetailEvent
    data object Dismiss : MatchDetailEvent
}

/**
 * ScreenModel for match detail screen.
 * Fetches and displays a single match's details.
 */
internal class MatchDetailStepModel(
    private val gameRepository: GameRepository,
    private val stringsHolder: GamesStringsHolder,
    private val matchId: String,
) : ScreenModel {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    init {
        loadMatch()
    }

    fun onEvent(event: MatchDetailEvent) {
        when (event) {
            MatchDetailEvent.Retry -> loadMatch()
            MatchDetailEvent.Dismiss -> {
                // TODO: Handled by Navigator back button
            }
        }
    }

    private fun loadMatch() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // TODO Phase 3-ETAPA3: Fetch from repository using matchId
            // For now, return empty to structure the UI properly.
            // When GetGameByIdUseCase is implemented, call it here.
            _state.update { it.copy(isLoading = false, match = null) }
        }
    }
}
