package com.walcker.games.features.ui.matchdetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for match detail screen.
 */
internal data class MatchDetailState(
    val match: Game? = null,
    val participants: ParticipantsSummary? = null,
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
 *
 * Subscribes to two live streams:
 * 1. The match document (status, counts) — updates the header/badges live.
 * 2. The participants subcollection — updates the confirmed/waitlist list.
 *
 * A one-shot [getGameById] seeds the initial state so the screen renders
 * immediately, then the live subscriptions take over.
 */
internal class MatchDetailStepModel(
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val matchId: String,
) : ScreenModel {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    init {
        loadMatch()
        subscribeToMatch()
        subscribeToParticipants()
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

            val result = getGameById(matchId)
            result.onSuccess { game ->
                _state.update { it.copy(isLoading = false, match = game) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error loading match details",
                    )
                }
            }
        }
    }

    private fun subscribeToMatch() {
        screenModelScope.launch {
            observeMatch(matchId)
                .catch { /* ignore flow errors - keep last known state */ }
                .collect { result ->
                    result.onSuccess { game ->
                        // Live update replaces the match; clears loading/error once we have data.
                        _state.update { it.copy(match = game, isLoading = false, errorMessage = null) }
                    }
                    // Errors from observation don't wipe the last good match.
                }
        }
    }

    private fun subscribeToParticipants() {
        screenModelScope.launch {
            observeParticipants(matchId)
                .catch { /* ignore flow errors - keep last known state */ }
                .collect { result ->
                    result.onSuccess { summary ->
                        _state.update { it.copy(participants = summary) }
                    }
                    // Errors from observation don't replace the existing match data
                }
        }
    }
}
