package com.walcker.games.features.ui.matchdetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.CancelMatchOutcome
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.JoinMatchOutcome
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.features.domain.model.SubmitRatingOutcome
import com.walcker.games.features.domain.model.SubmitReportOutcome
import com.walcker.games.features.domain.model.canBeRatedBy
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.usecase.SubmitReportUseCase
import com.walcker.games.features.ui.notifications.getCurrentTimeMillis
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.JoinOutcome
import com.walcker.match.navigator.PromotionCoordinator
import com.walcker.match.navigator.PromotionNotice
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MatchDetailState(
    val match: Game? = null,
    val participants: ParticipantsSummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val justPromoted: Boolean = false,
    val isJoining: Boolean = false,
    val joinOutcome: JoinMatchOutcome? = null,
    val successMessage: String? = null,
    val isLeavingMatch: Boolean = false,
    val isCancellingMatch: Boolean = false,
    val showLeaveConfirmDialog: Boolean = false,
    val showCancelConfirmDialog: Boolean = false,
    val statusChangeMessage: String? = null,
    val showRatingSheet: Boolean = false,
    val selectedPlayerForRating: Pair<String, String>? = null,
    val isSubmittingRating: Boolean = false,
    val ratingErrorMessage: String? = null,
    val showReportSheet: Boolean = false,
    val selectedPlayerForReport: Pair<String, String>? = null,
    val isSubmittingReport: Boolean = false,
    val reportErrorMessage: String? = null,
    val currentUserId: String? = null,
    val canRate: Boolean = false,
    val isMatchOver: Boolean = false,
)

internal sealed interface MatchDetailEvent {
    data object Retry : MatchDetailEvent
    data object Dismiss : MatchDetailEvent
    data object DismissPromotion : MatchDetailEvent
    data object JoinMatch : MatchDetailEvent
    data object DismissSuccess : MatchDetailEvent
    data object DismissStatusChange : MatchDetailEvent
    data object RequestLeaveMatch : MatchDetailEvent
    data object ConfirmLeaveMatch : MatchDetailEvent
    data object CancelLeaveMatch : MatchDetailEvent
    data object RequestCancelMatch : MatchDetailEvent
    data object ConfirmCancelMatch : MatchDetailEvent
    data object CancelCancelMatch : MatchDetailEvent
    data class OpenRatingSheet(val userId: String, val displayName: String) : MatchDetailEvent
    data object CloseRatingSheet : MatchDetailEvent
    data class SubmitRating(
        val rating: Int,
        val comment: String,
        val dimensions: RatingDimensions,
    ) : MatchDetailEvent
    data object DismissRatingError : MatchDetailEvent
    data class OpenReportSheet(val userId: String, val displayName: String) : MatchDetailEvent
    data object CloseReportSheet : MatchDetailEvent
    data class SubmitReport(val reason: ReportReason, val details: String) : MatchDetailEvent
    data object DismissReportError : MatchDetailEvent
}

internal sealed interface MatchDetailEffect {
    data class NavigateToConfirmation(
        val matchId: String,
        val venueName: String,
        val startsAtSeconds: Long,
        val sportLabel: String,
    ) : MatchDetailEffect

    data object RequireLogin : MatchDetailEffect
}

internal class MatchDetailStepModel(
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val joinGame: JoinGameUseCase,
    private val leaveMatch: LeaveMatchUseCase,
    private val cancelMatch: CancelMatchUseCase,
    private val submitRating: SubmitRatingUseCase,
    private val submitReport: SubmitReportUseCase,
    private val sessionHolder: SessionHolder,
    private val promotionCoordinator: PromotionCoordinator,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val matchId: String,
    private val nowSeconds: () -> Long = {
        kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
    },
) : ScreenModel {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    private val _effects = Channel<MatchDetailEffect>(Channel.BUFFERED)
    val effects: Flow<MatchDetailEffect> = _effects.receiveAsFlow()

    private var previousConfirmedIds: Set<String>? = null
    private var currentUserId: String? = null
    private var previousStatus: MatchStatus? = null

    private var viewTracked = false

    private fun MatchDetailState.withCanRate(): MatchDetailState {
        val game = match ?: return copy(canRate = false, isMatchOver = false)
        val now = nowSeconds()
        return copy(
            isMatchOver = game.isOver(now),
            canRate = game.canBeRatedBy(userId = currentUserId, nowSeconds = now),
        )
    }

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
                submitPlayerRating(event.rating, event.comment, event.dimensions)
            }
            is MatchDetailEvent.DismissRatingError -> {
                _state.update { it.copy(ratingErrorMessage = null) }
            }
            is MatchDetailEvent.OpenReportSheet -> {
                _state.update {
                    it.copy(
                        showReportSheet = true,
                        selectedPlayerForReport = event.userId to event.displayName,
                        reportErrorMessage = null,
                    )
                }
            }
            is MatchDetailEvent.CloseReportSheet -> {
                _state.update {
                    it.copy(showReportSheet = false, selectedPlayerForReport = null)
                }
            }
            is MatchDetailEvent.SubmitReport -> {
                submitPlayerReport(event.reason, event.details)
            }
            is MatchDetailEvent.DismissReportError -> {
                _state.update { it.copy(reportErrorMessage = null) }
            }
        }
    }

    private fun submitPlayerReport(reason: ReportReason, details: String) {
        val reportedUserId = _state.value.selectedPlayerForReport?.first ?: return
        val strings = stringsHolder.resolveStringsOrDefault().reports

        screenModelScope.launch {
            _state.update { it.copy(isSubmittingReport = true, reportErrorMessage = null) }

            submitReport(
                matchId = matchId,
                reportedUserId = reportedUserId,
                reason = reason,
                details = details,
            ).onSuccess { outcome ->
                val message = when (outcome) {
                    SubmitReportOutcome.Recorded -> strings.success
                    SubmitReportOutcome.AlreadyReported -> strings.alreadyReported
                }
                _state.update {
                    it.copy(
                        isSubmittingReport = false,
                        showReportSheet = false,
                        selectedPlayerForReport = null,
                        successMessage = message,
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(isSubmittingReport = false, reportErrorMessage = strings.error)
                }
            }
        }
    }

    private fun submitPlayerRating(
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ) {
        val ratedUserId = _state.value.selectedPlayerForRating?.first ?: return
        val strings = stringsHolder.resolveStringsOrDefault().ratings

        screenModelScope.launch {
            _state.update { it.copy(isSubmittingRating = true, ratingErrorMessage = null) }
            submitRating(
                matchId = matchId,
                ratedUserId = ratedUserId,
                rating = rating,
                comment = comment,
                dimensions = dimensions,
            ).onSuccess { outcome ->
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
                _state.update {
                    it.copy(
                        isSubmittingRating = false,
                        ratingErrorMessage = strings.submitError,
                    )
                }
            }
        }
    }

    private fun trackViewOnce(game: Game) {
        if (viewTracked) return
        viewTracked = true
        analytics.track(
            AnalyticsEvent.MatchViewed(
                sport = game.sport.name,
                hasOpenSlots = game.hasOpenSlots,
            ),
        )
    }

    private fun joinMatchAction() {
        val sport = _state.value.match?.sport?.name ?: UNKNOWN_SPORT
        val strings = stringsHolder.resolveStringsOrDefault().matchDetail

        screenModelScope.launch {
            if (sessionHolder.currentUser.first() == null) {
                _effects.send(MatchDetailEffect.RequireLogin)
                return@launch
            }

            _state.update { it.copy(isJoining = true, errorMessage = null) }
            analytics.track(AnalyticsEvent.MatchJoinAttempted(sport))

            joinGame(matchId)
                .onSuccess { outcome ->
                    analytics.track(
                        AnalyticsEvent.MatchJoinResult(
                            sport = sport,
                            outcome = when (outcome) {
                                is JoinMatchOutcome.Confirmed -> JoinOutcome.CONFIRMED
                                is JoinMatchOutcome.Waitlist -> JoinOutcome.WAITLIST
                                is JoinMatchOutcome.AlreadyJoined -> JoinOutcome.CONFIRMED
                            },
                        ),
                    )
                    val message = when (outcome) {
                        is JoinMatchOutcome.Confirmed -> null
                        is JoinMatchOutcome.Waitlist -> strings.joinWaitlistSuccess(outcome.position)
                        is JoinMatchOutcome.AlreadyJoined -> strings.joinAlreadyJoined
                    }
                    _state.update {
                        it.copy(
                            isJoining = false,
                            joinOutcome = outcome,
                            successMessage = message,
                        )
                    }
                    if (outcome is JoinMatchOutcome.Confirmed) {
                        _state.value.match?.let { match ->
                            _effects.send(
                                MatchDetailEffect.NavigateToConfirmation(
                                    matchId = matchId,
                                    venueName = match.venueName,
                                    startsAtSeconds = match.startsAtSeconds,
                                    sportLabel = match.sport.label,
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    analytics.track(
                        AnalyticsEvent.MatchJoinResult(sport, JoinOutcome.FAILED),
                    )
                    _state.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = strings.joinError,
                        )
                    }
                }
        }
    }

    private fun leaveMatchAction() {
        val strings = stringsHolder.resolveStringsOrDefault().matchDetail
        screenModelScope.launch {
            _state.update { it.copy(isLeavingMatch = true, showLeaveConfirmDialog = false, errorMessage = null) }

            leaveMatch(matchId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLeavingMatch = false,
                            successMessage = strings.leaveSuccess,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLeavingMatch = false,
                            errorMessage = strings.leaveError,
                        )
                    }
                }
        }
    }

    private fun cancelMatchAction() {
        val strings = stringsHolder.resolveStringsOrDefault().matchDetail
        screenModelScope.launch {
            _state.update { it.copy(isCancellingMatch = true, showCancelConfirmDialog = false, errorMessage = null) }

            cancelMatch(matchId)
                .onSuccess { outcome ->
                    val message = when (outcome) {
                        is CancelMatchOutcome.Cancelled -> strings.cancelSuccess
                        is CancelMatchOutcome.AlreadyCancelled -> strings.cancelAlreadyCancelled
                    }
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            successMessage = message,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            errorMessage = strings.cancelError,
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
                trackViewOnce(game)
                _state.update { it.copy(isLoading = false, match = game).withCanRate() }
            }.onFailure {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = stringsHolder.resolveStringsOrDefault().matchDetail.loadError,
                    )
                }
            }
        }
    }

    private fun subscribeToMatch() {
        screenModelScope.launch {
            observeMatch(matchId)
                .catch { }
                .collect { result ->
                    result.onSuccess { game ->
                        trackViewOnce(game)
                        detectStatusChange(game.status)
                        _state.update {
                            it.copy(match = game, isLoading = false, errorMessage = null)
                                .withCanRate()
                        }
                    }
                }
        }
    }

    private fun subscribeToParticipants() {
        screenModelScope.launch {
            if (currentUserId == null) {
                currentUserId = sessionHolder.currentUser.first()?.uid
                _state.update { it.copy(currentUserId = currentUserId).withCanRate() }
            }

            observeParticipants(matchId)
                .catch { }
                .collect { result ->
                    result.onSuccess { summary ->
                        detectPromotion(summary)
                        _state.update { it.copy(participants = summary) }
                    }
                }
        }
    }

    private fun detectPromotion(summary: ParticipantsSummary) {
        val userId = currentUserId ?: return
        val previousIds = previousConfirmedIds ?: run {
            previousConfirmedIds = summary.confirmed.map { it.userId }.toSet()
            return
        }

        val newConfirmedIds = summary.confirmed.map { it.userId }.toSet()
        val justAdded = newConfirmedIds - previousIds

        if (userId in justAdded) {
            _state.update { it.copy(justPromoted = true) }
            val strings = stringsHolder.resolveStringsOrDefault().matchDetail
            val matchTitle = buildString {
                append(_state.value.match?.sport?.label ?: strings.unknownMatchTitle)
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

    private fun detectStatusChange(currentStatus: MatchStatus) {
        val prevStatus = previousStatus ?: run {
            previousStatus = currentStatus
            return
        }

        if (prevStatus != currentStatus) {
            val strings = stringsHolder.resolveStringsOrDefault().matchDetail
            val message = when {
                prevStatus == MatchStatus.OPEN &&
                currentStatus == MatchStatus.FULL -> strings.statusChangedToFull
                currentStatus == MatchStatus.FINISHED -> strings.statusChangedToFinished
                currentStatus == MatchStatus.CANCELLED -> strings.statusChangedToCancelled
                else -> null
            }

            if (message != null) {
                _state.update { it.copy(statusChangeMessage = message) }
            }
        }

        previousStatus = currentStatus
    }

    private companion object {
        const val UNKNOWN_SPORT = "unknown"
    }
}
