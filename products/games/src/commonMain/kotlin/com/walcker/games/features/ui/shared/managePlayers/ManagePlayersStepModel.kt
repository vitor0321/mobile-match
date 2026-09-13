package com.walcker.games.features.ui.shared.managePlayers

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.model.canOrganizerRate
import com.walcker.games.features.domain.shared.repository.PlayerRepository
import com.walcker.games.features.domain.shared.repository.RatingRepository
import com.walcker.games.features.domain.shared.usecase.BanPlayerFromMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ConfirmWaitlistedPlayerUseCase
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.SetVipStatusUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ManagePlayersState(
    val match: Game? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserId: String? = null,
    val canManage: Boolean = false,
    val confirmedPlayers: List<Participant> = emptyList(),
    val participantRatings: Map<String, PlayerRatingSummary> = emptyMap(),
    val organizerRatingsGiven: Map<String, Rating> = emptyMap(),
    val showRatingSheet: Boolean = false,
    val selectedPlayerForRating: Pair<String, String>? = null,
    val existingRatingForSelectedPlayer: Rating? = null,
    val isSubmittingRating: Boolean = false,
    val ratingErrorMessage: String? = null,
    val ratingSuccessMessage: String? = null,
    val waitlistPlayers: List<Participant> = emptyList(),
    val isUpdatingVip: Boolean = false,
    val isConfirmingWaitlisted: Boolean = false,
    val playerPendingBan: Pair<String, String>? = null,
    val isBanningPlayer: Boolean = false,
)

internal sealed interface ManagePlayersEvent {
    data object Retry : ManagePlayersEvent

    data object DismissActionError : ManagePlayersEvent

    data object DismissSuccess : ManagePlayersEvent

    data class ToggleVip(
        val userId: String,
        val displayName: String,
        val currentlyVip: Boolean,
    ) : ManagePlayersEvent

    data class ConfirmWaitlisted(
        val userId: String,
        val displayName: String,
    ) : ManagePlayersEvent

    data class RequestBan(
        val userId: String,
        val displayName: String,
    ) : ManagePlayersEvent

    data object ConfirmBan : ManagePlayersEvent

    data object CancelBan : ManagePlayersEvent

    data class OpenRatingSheet(
        val userId: String,
        val displayName: String,
    ) : ManagePlayersEvent

    data object CloseRatingSheet : ManagePlayersEvent

    data class SubmitRating(
        val rating: Int,
        val comment: String,
        val reportReason: ReportReason?,
        val reportDetails: String,
    ) : ManagePlayersEvent

    data object DismissRatingError : ManagePlayersEvent

    data object DismissRatingSuccess : ManagePlayersEvent
}

internal class ManagePlayersStepModel(
    private val matchId: String,
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val submitRating: SubmitRatingUseCase,
    private val submitReport: SubmitReportUseCase,
    private val setVipStatus: SetVipStatusUseCase,
    private val confirmWaitlistedPlayer: ConfirmWaitlistedPlayerUseCase,
    private val banPlayerFromMatch: BanPlayerFromMatchUseCase,
    private val playerRepository: PlayerRepository,
    private val ratingRepository: RatingRepository,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(ManagePlayersState())
    val state: StateFlow<ManagePlayersState> = _state.asStateFlow()

    private var currentUserId: String? = null

    private fun ManagePlayersState.withCanManage(): ManagePlayersState {
        val game = match ?: return copy(canManage = false)
        return copy(canManage = game.canOrganizerRate(userId = currentUserId))
    }

    init {
        loadMatch()
        subscribeToMatch()
        subscribeToCurrentUser()
    }

    fun onEvent(event: ManagePlayersEvent) {
        when (event) {
            ManagePlayersEvent.Retry -> loadMatch()
            ManagePlayersEvent.DismissActionError -> {
                _state.update { it.copy(actionErrorMessage = null) }
            }
            ManagePlayersEvent.DismissSuccess -> {
                _state.update { it.copy(successMessage = null) }
            }
            is ManagePlayersEvent.ToggleVip -> toggleVipAction(event.userId, event.displayName, event.currentlyVip)
            is ManagePlayersEvent.ConfirmWaitlisted -> confirmWaitlistedAction(event.userId, event.displayName)
            is ManagePlayersEvent.RequestBan -> {
                _state.update { it.copy(playerPendingBan = event.userId to event.displayName) }
            }
            ManagePlayersEvent.ConfirmBan -> banPlayerAction()
            ManagePlayersEvent.CancelBan -> {
                _state.update { it.copy(playerPendingBan = null) }
            }
            is ManagePlayersEvent.OpenRatingSheet -> {
                _state.update {
                    it.copy(
                        showRatingSheet = true,
                        selectedPlayerForRating = event.userId to event.displayName,
                        existingRatingForSelectedPlayer = it.organizerRatingsGiven[event.userId],
                    )
                }
            }
            ManagePlayersEvent.CloseRatingSheet -> {
                _state.update {
                    it.copy(showRatingSheet = false, selectedPlayerForRating = null, existingRatingForSelectedPlayer = null)
                }
            }
            is ManagePlayersEvent.SubmitRating -> {
                submitPlayerRating(event.rating, event.comment, event.reportReason, event.reportDetails)
            }
            ManagePlayersEvent.DismissRatingError -> {
                _state.update { it.copy(ratingErrorMessage = null) }
            }
            ManagePlayersEvent.DismissRatingSuccess -> {
                _state.update { it.copy(ratingSuccessMessage = null) }
            }
        }
    }

    private fun loadMatch() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getGameById(matchId)
                .onSuccess { game ->
                    _state.update { it.copy(isLoading = false, match = game).withCanManage() }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = stringsHolder.resolveStringsOrDefault().manageMatch.notFound,
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
                        _state.update { it.copy(match = game, isLoading = false).withCanManage() }
                    }
                }
        }
    }

    private fun subscribeToCurrentUser() {
        screenModelScope.launch {
            currentUserId = sessionHolder.currentUser.first()?.uid
            _state.update { it.copy(currentUserId = currentUserId).withCanManage() }
            loadOrganizerRatingsGiven()
            subscribeToParticipants()
        }
    }

    private fun subscribeToParticipants() {
        screenModelScope.launch {
            observeParticipants(matchId)
                .catch { }
                .collect { result ->
                    result.onSuccess { summary ->
                        _state.update { it.copy(confirmedPlayers = summary.confirmed, waitlistPlayers = summary.waitlist) }
                        loadParticipantRatings(summary.confirmed.map { it.userId })
                    }
                }
        }
    }

    private fun loadParticipantRatings(userIds: List<String>) {
        if (userIds.isEmpty()) return
        screenModelScope.launch {
            playerRepository.getPlayersRatingSummary(userIds).onSuccess { ratings ->
                _state.update { it.copy(participantRatings = ratings) }
            }
        }
    }

    private fun loadOrganizerRatingsGiven() {
        val organizerId = currentUserId ?: return
        screenModelScope.launch {
            ratingRepository.getRatingsGivenForMatch(matchId, organizerId).onSuccess { ratings ->
                _state.update {
                    it.copy(organizerRatingsGiven = ratings.associateBy { rating -> rating.ratedUserId })
                }
            }
        }
    }

    private fun submitPlayerRating(
        rating: Int,
        comment: String,
        reportReason: ReportReason?,
        reportDetails: String,
    ) {
        val ratedUserId = _state.value.selectedPlayerForRating?.first ?: return
        val ratingStrings = stringsHolder.resolveStringsOrDefault().ratings
        val reportStrings = stringsHolder.resolveStringsOrDefault().reports

        screenModelScope.launch {
            _state.update { it.copy(isSubmittingRating = true, ratingErrorMessage = null) }
            submitRating(
                matchId = matchId,
                ratedUserId = ratedUserId,
                rating = rating,
                comment = comment,
            ).onSuccess { outcome ->
                analytics.track(AnalyticsEvent.PlayerRated(rating))
                loadOrganizerRatingsGiven()

                var message =
                    when (outcome) {
                        is SubmitRatingOutcome.Recorded -> ratingStrings.submitSuccess
                        is SubmitRatingOutcome.Updated -> ratingStrings.updated
                        is SubmitRatingOutcome.AlreadyRated -> ratingStrings.updated
                    }

                if (reportReason != null) {
                    submitReport(matchId, ratedUserId, reportReason, reportDetails)
                        .onSuccess { reportOutcome ->
                            analytics.track(AnalyticsEvent.PlayerReported(reportReason.name))
                            val reportMessage =
                                when (reportOutcome) {
                                    SubmitReportOutcome.Recorded -> reportStrings.success
                                    SubmitReportOutcome.AlreadyReported -> reportStrings.alreadyReported
                                }
                            message = "$message $reportMessage"
                        }.onFailure { error -> crashReporter.recordException(error) }
                }

                _state.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet = false,
                        selectedPlayerForRating = null,
                        ratingSuccessMessage = message,
                        participantRatings =
                            it.participantRatings +
                                (
                                    ratedUserId to
                                        PlayerRatingSummary(
                                            rating = outcome.averageRating,
                                            ratingCount = outcome.ratingCount,
                                        )
                                ),
                    )
                }
            }.onFailure { error ->
                crashReporter.recordException(error)
                _state.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet = false,
                        selectedPlayerForRating = null,
                        ratingErrorMessage = ratingStrings.submitError,
                    )
                }
            }
        }
    }

    private fun toggleVipAction(
        userId: String,
        displayName: String,
        currentlyVip: Boolean,
    ) {
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        val newIsVip = !currentlyVip
        screenModelScope.launch {
            _state.update { it.copy(isUpdatingVip = true, actionErrorMessage = null) }
            setVipStatus(matchId, userId, newIsVip)
                .onSuccess {
                    val message = if (newIsVip) strings.vipSuccessMessage(displayName) else strings.vipRemovedMessage(displayName)
                    _state.update { it.copy(isUpdatingVip = false, successMessage = message) }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update { it.copy(isUpdatingVip = false, actionErrorMessage = strings.vipError) }
                }
        }
    }

    private fun confirmWaitlistedAction(
        userId: String,
        displayName: String,
    ) {
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update { it.copy(isConfirmingWaitlisted = true, actionErrorMessage = null) }
            confirmWaitlistedPlayer(matchId, userId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isConfirmingWaitlisted = false,
                            successMessage = strings.confirmWaitlistedSuccessMessage(displayName),
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update { it.copy(isConfirmingWaitlisted = false, actionErrorMessage = strings.confirmWaitlistedError) }
                }
        }
    }

    private fun banPlayerAction() {
        val (userId, displayName) = _state.value.playerPendingBan ?: return
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update { it.copy(isBanningPlayer = true, actionErrorMessage = null) }
            banPlayerFromMatch(matchId, userId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isBanningPlayer = false,
                            playerPendingBan = null,
                            successMessage = strings.banSuccessMessage(displayName),
                            confirmedPlayers = it.confirmedPlayers.filterNot { player -> player.userId == userId },
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(isBanningPlayer = false, playerPendingBan = null, actionErrorMessage = strings.banError)
                    }
                }
        }
    }
}
