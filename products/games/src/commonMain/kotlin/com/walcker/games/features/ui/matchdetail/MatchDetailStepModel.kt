package com.walcker.games.features.ui.matchdetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.model.SubmitRatingOutcome
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.usecase.SubmitRatingUseCase
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
    // Join match state
    val isJoining: Boolean = false,
    val joinOutcome: JoinMatchOutcome? = null,
    val successMessage: String? = null,
    // Leave/Cancel states
    val isLeavingMatch: Boolean = false,
    val isCancellingMatch: Boolean = false,
    val showLeaveConfirmDialog: Boolean = false,
    val showCancelConfirmDialog: Boolean = false,
    // Real-time status change notifications
    val statusChangeMessage: String? = null,
    // Rating UI state
    val showRatingSheet: Boolean = false,
    val selectedPlayerForRating: Pair<String, String>? = null, // userId to displayName
    val isSubmittingRating: Boolean = false,
    /**
     * Failure of the rating submission only. Kept apart from [errorMessage]
     * because that one replaces the whole screen with a retry page — a review
     * that did not go through must not blank the match the user is looking at.
     */
    val ratingErrorMessage: String? = null,
)

/**
 * Events for match detail screen.
 */
internal sealed interface MatchDetailEvent {
    data object Retry : MatchDetailEvent
    data object Dismiss : MatchDetailEvent
    /** Dismisses the "you were promoted" banner. */
    data object DismissPromotion : MatchDetailEvent
    /** Joins the match. */
    data object JoinMatch : MatchDetailEvent
    /** Dismisses the success message. */
    data object DismissSuccess : MatchDetailEvent
    /** Dismisses the status change notification. */
    data object DismissStatusChange : MatchDetailEvent
    /** Shows leave match confirmation dialog. */
    data object RequestLeaveMatch : MatchDetailEvent
    /** Confirms leaving the match. */
    data object ConfirmLeaveMatch : MatchDetailEvent
    /** Closes leave match confirmation dialog. */
    data object CancelLeaveMatch : MatchDetailEvent
    /** Shows cancel match confirmation dialog. */
    data object RequestCancelMatch : MatchDetailEvent
    /** Confirms cancelling the match. */
    data object ConfirmCancelMatch : MatchDetailEvent
    /** Closes cancel match confirmation dialog. */
    data object CancelCancelMatch : MatchDetailEvent
    /** Opens rating sheet for a specific player. */
    data class OpenRatingSheet(val userId: String, val displayName: String) : MatchDetailEvent
    /** Closes rating sheet. */
    data object CloseRatingSheet : MatchDetailEvent
    /** Submits a rating for a player. */
    data class SubmitRating(val rating: Int, val comment: String) : MatchDetailEvent
    /** Dismisses the rating failure banner. */
    data object DismissRatingError : MatchDetailEvent
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
    private val joinGame: JoinGameUseCase,
    private val leaveMatch: LeaveMatchUseCase,
    private val cancelMatch: CancelMatchUseCase,
    private val submitRating: SubmitRatingUseCase,
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
    /**
     * Previous match status; used to detect status transitions.
     * `null` until the first snapshot lands, so we don't fire on initial load.
     */
    private var previousStatus: com.walcker.games.features.domain.model.MatchStatus? = null

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
            MatchDetailEvent.JoinMatch -> joinMatchAction()
            MatchDetailEvent.DismissSuccess -> {
                _state.update { it.copy(successMessage = null, joinOutcome = null) }
            }
            MatchDetailEvent.DismissStatusChange -> {
                _state.update { it.copy(statusChangeMessage = null) }
            }
            MatchDetailEvent.RequestLeaveMatch -> {
                _state.update { it.copy(showLeaveConfirmDialog = true) }
            }
            MatchDetailEvent.ConfirmLeaveMatch -> leaveMatchAction()
            MatchDetailEvent.CancelLeaveMatch -> {
                _state.update { it.copy(showLeaveConfirmDialog = false) }
            }
            MatchDetailEvent.RequestCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = true) }
            }
            MatchDetailEvent.ConfirmCancelMatch -> cancelMatchAction()
            MatchDetailEvent.CancelCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = false) }
            }
            is MatchDetailEvent.OpenRatingSheet -> {
                _state.update {
                    it.copy(
                        showRatingSheet = true,
                        selectedPlayerForRating = event.userId to event.displayName,
                    )
                }
            }
            MatchDetailEvent.CloseRatingSheet -> {
                _state.update {
                    it.copy(showRatingSheet = false, selectedPlayerForRating = null)
                }
            }
            is MatchDetailEvent.SubmitRating -> {
                submitPlayerRating(event.rating, event.comment)
            }
            is MatchDetailEvent.DismissRatingError -> {
                _state.update { it.copy(ratingErrorMessage = null) }
            }
        }
    }

    private fun submitPlayerRating(rating: Int, comment: String) {
        val ratedUserId = _state.value.selectedPlayerForRating?.first ?: return
        val strings = stringsHolder.resolveStringsOrDefault().ratings

        screenModelScope.launch {
            _state.update { it.copy(isSubmittingRating = true, ratingErrorMessage = null) }
            submitRating(
                matchId = matchId,
                ratedUserId = ratedUserId,
                rating = rating,
                comment = comment,
            ).onSuccess { outcome ->
                // Resending is idempotent server-side. From the person who
                // tapped the button, both outcomes mean "it is registered" —
                // only the wording differs.
                val message = when (outcome) {
                    is SubmitRatingOutcome.Recorded -> strings.submitSuccess
                    is SubmitRatingOutcome.AlreadyRated -> strings.alreadyRated
                }
                _state.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet = false,
                        selectedPlayerForRating = null,
                        successMessage = message,
                    )
                }
            }.onFailure {
                // The callable transport drops the HttpsError code, so mapping
                // the server text to a specific message would be guesswork.
                // The sheet stays open so the person can retry without
                // re-selecting the player.
                _state.update {
                    it.copy(
                        isSubmittingRating = false,
                        ratingErrorMessage = strings.submitError,
                    )
                }
            }
        }
    }

    private fun joinMatchAction() {
        screenModelScope.launch {
            _state.update { it.copy(isJoining = true, errorMessage = null) }

            joinGame(matchId)
                .onSuccess { outcome ->
                    val message = when (outcome) {
                        is JoinMatchOutcome.Confirmed -> "Você entrou na partida! ✓"
                        is JoinMatchOutcome.Waitlist -> "Você foi adicionado à fila de espera (posição #${outcome.position})"
                        is JoinMatchOutcome.AlreadyJoined -> "Você já é participante desta partida"
                    }
                    _state.update {
                        it.copy(
                            isJoining = false,
                            joinOutcome = outcome,
                            successMessage = message,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = error.message ?: "Erro ao entrar na partida",
                        )
                    }
                }
        }
    }

    private fun leaveMatchAction() {
        screenModelScope.launch {
            _state.update { it.copy(isLeavingMatch = true, showLeaveConfirmDialog = false, errorMessage = null) }

            leaveMatch(matchId)
                .onSuccess { outcome ->
                    val message = "Você saiu da partida"
                    _state.update {
                        it.copy(
                            isLeavingMatch = false,
                            successMessage = message,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLeavingMatch = false,
                            errorMessage = error.message ?: "Erro ao sair da partida",
                        )
                    }
                }
        }
    }

    private fun cancelMatchAction() {
        screenModelScope.launch {
            _state.update { it.copy(isCancellingMatch = true, showCancelConfirmDialog = false, errorMessage = null) }

            cancelMatch(matchId)
                .onSuccess { outcome ->
                    val message = when (outcome) {
                        is CancelMatchOutcome.Cancelled -> "Partida cancelada"
                        is CancelMatchOutcome.AlreadyCancelled -> "Partida já foi cancelada"
                    }
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            successMessage = message,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            errorMessage = error.message ?: "Erro ao cancelar partida",
                        )
                    }
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
                        detectStatusChange(game.status)
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

    /**
     * Detects match status transitions and shows appropriate notifications.
     * Fires only when status actually changes, not on first load.
     */
    private fun detectStatusChange(currentStatus: com.walcker.games.features.domain.model.MatchStatus) {
        val prevStatus = previousStatus ?: run {
            // First snapshot — record the baseline but don't fire.
            previousStatus = currentStatus
            return
        }

        // Detect transitions and show appropriate messages
        if (prevStatus != currentStatus) {
            val message = when {
                // Match became full
                prevStatus == com.walcker.games.features.domain.model.MatchStatus.OPEN &&
                currentStatus == com.walcker.games.features.domain.model.MatchStatus.FULL -> {
                    "Partida lotada! 🔴 Novas entradas serão na fila de espera."
                }
                // Match was finished
                currentStatus == com.walcker.games.features.domain.model.MatchStatus.FINISHED -> {
                    "Partida encerrada ✓"
                }
                // Match was cancelled
                currentStatus == com.walcker.games.features.domain.model.MatchStatus.CANCELLED -> {
                    "Partida foi cancelada ✕"
                }
                // Any other transition
                else -> "Status da partida foi atualizado: ${currentStatus.name}"
            }

            _state.update { it.copy(statusChangeMessage = message) }
        }

        previousStatus = currentStatus
    }
}
