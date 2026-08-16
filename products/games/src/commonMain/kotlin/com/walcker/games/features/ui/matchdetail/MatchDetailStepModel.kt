package com.walcker.games.features.ui.matchdetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.ui.notifications.getCurrentTimeMillis
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.navigator.PromotionCoordinator
import com.walcker.match.navigator.PromotionNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    /**
     * Set when the logged-in user was just promoted from waitlist to confirmed.
     * The UI uses this to show a transient banner ("Você foi promovido!").
     */
    val justPromoted: Boolean = false,
)

/**
 * Events for match detail screen.
 */
internal sealed interface MatchDetailEvent {
    data object Retry : MatchDetailEvent
    data object Dismiss : MatchDetailEvent
    /** Dismisses the "you were promoted" banner. */
    data object DismissPromotion : MatchDetailEvent
}

/**
 * ScreenModel for match detail screen.
 *
 * Subscribes to two live streams:
 * 1. The match document (status, counts) — updates the header/badges live.
 * 2. The participants subcollection — updates the confirmed/waitlist list.
 *
 * Detects when the logged-in user transitions from waitlist to confirmed
 * (someone cancelled, first FIFO was promoted) and emits a [PromotionEvent]
 * via [PromotionCoordinator] so the shell can show a global banner.
 */
internal class MatchDetailStepModel(
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val sessionHolder: SessionHolder,
    private val promotionCoordinator: PromotionCoordinator,
    private val stringsHolder: GamesStringsHolder,
    private val matchId: String,
) : ScreenModel {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    /**
     * Last-known confirmed-set userIds; used to detect promotion transitions.
     * `null` until the first snapshot lands, so we don't fire on initial load.
     */
    private var previousConfirmedIds: Set<String>? = null
    private var currentUserId: String? = null

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
            MatchDetailEvent.DismissPromotion -> {
                _state.update { it.copy(justPromoted = false) }
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
                        _state.update { it.copy(match = game, isLoading = false, errorMessage = null) }
                    }
                }
        }
    }

    private fun subscribeToParticipants() {
        screenModelScope.launch {
            // Resolve the current user once; subsequent emissions use the same id.
            if (currentUserId == null) {
                currentUserId = sessionHolder.currentUser.first()?.uid
            }

            observeParticipants(matchId)
                .catch { /* ignore flow errors - keep last known state */ }
                .collect { result ->
                    result.onSuccess { summary ->
                        detectPromotion(summary)
                        _state.update { it.copy(participants = summary) }
                    }
                }
        }
    }

    /**
     * Compares the new confirmed-set with the previous one. If the current
     * user was previously in the waitlist (not in previousConfirmedIds) and
     * is now in the confirmed set, we have a promotion.
     */
    private fun detectPromotion(summary: ParticipantsSummary) {
        val userId = currentUserId ?: return
        val previousIds = previousConfirmedIds ?: run {
            // First snapshot — record the baseline but don't fire.
            previousConfirmedIds = summary.confirmed.map { it.userId }.toSet()
            return
        }

        val newConfirmedIds = summary.confirmed.map { it.userId }.toSet()
        val justAdded = newConfirmedIds - previousIds

        if (userId in justAdded) {
            // We just got promoted — flip the in-screen flag and emit global event.
            _state.update { it.copy(justPromoted = true) }
            val matchTitle = buildString {
                append(_state.value.match?.sport?.label ?: "Partida")
                append(" · ")
                append(_state.value.match?.venueName ?: "")
            }
            promotionCoordinator.emit(
                PromotionNotice(
                    matchId = matchId,
                    matchTitle = matchTitle,
                    promotedAtMs = getCurrentTimeMillis(),
                )
            )
        }

        previousConfirmedIds = newConfirmedIds
    }
}
