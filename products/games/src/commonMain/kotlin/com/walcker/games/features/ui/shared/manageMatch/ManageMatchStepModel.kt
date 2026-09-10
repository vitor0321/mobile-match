package com.walcker.games.features.ui.shared.manageMatch

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
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCase
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.SetTeamAssignmentsUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCase
import com.walcker.games.features.domain.shared.util.shuffleIntoTeams
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
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

internal data class ManageMatchState(
    val match: Game? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val successMessage: String? = null,
    val isCancellingMatch: Boolean = false,
    val showCancelConfirmDialog: Boolean = false,
    val isCancellingSeries: Boolean = false,
    val showCancelSeriesConfirmDialog: Boolean = false,
    val currentUserId: String? = null,
    val canRatePlayers: Boolean = false,
    val confirmedPlayers: List<Participant> = emptyList(),
    val selectedTeamCount: Int = 2,
    val isSavingTeams: Boolean = false,
    val participantRatings: Map<String, PlayerRatingSummary> = emptyMap(),
    val organizerRatingsGiven: Map<String, Rating> = emptyMap(),
    val showRatingSheet: Boolean = false,
    val selectedPlayerForRating: Pair<String, String>? = null,
    val existingRatingForSelectedPlayer: Rating? = null,
    val isSubmittingRating: Boolean = false,
    val ratingErrorMessage: String? = null,
    val ratingSuccessMessage: String? = null,
)

internal sealed interface ManageMatchEvent {
    data object Retry : ManageMatchEvent

    data object DismissActionError : ManageMatchEvent

    data object DismissSuccess : ManageMatchEvent

    data object RequestCancelMatch : ManageMatchEvent

    data object ConfirmCancelMatch : ManageMatchEvent

    data object CancelCancelMatch : ManageMatchEvent

    data object RequestCancelSeries : ManageMatchEvent

    data object ConfirmCancelSeries : ManageMatchEvent

    data object CancelCancelSeries : ManageMatchEvent

    data class TeamCountSelected(
        val teamCount: Int,
    ) : ManageMatchEvent

    data object ShuffleTeams : ManageMatchEvent

    data class MovePlayerToTeam(
        val userId: String,
        val teamIndex: Int,
    ) : ManageMatchEvent

    data class OpenRatingSheet(
        val userId: String,
        val displayName: String,
    ) : ManageMatchEvent

    data object CloseRatingSheet : ManageMatchEvent

    data class SubmitRating(
        val rating: Int,
        val comment: String,
        val reportReason: ReportReason?,
        val reportDetails: String,
    ) : ManageMatchEvent

    data object DismissRatingError : ManageMatchEvent

    data object DismissRatingSuccess : ManageMatchEvent
}

internal sealed interface ManageMatchEffect {
    data object MatchCancelled : ManageMatchEffect
}

internal class ManageMatchStepModel(
    private val matchId: String,
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val cancelMatch: CancelMatchUseCase,
    private val cancelMatchSeries: CancelMatchSeriesUseCase,
    private val setTeamAssignments: SetTeamAssignmentsUseCase,
    private val submitRating: SubmitRatingUseCase,
    private val submitReport: SubmitReportUseCase,
    private val playerRepository: PlayerRepository,
    private val ratingRepository: RatingRepository,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(ManageMatchState())
    val state: StateFlow<ManageMatchState> = _state.asStateFlow()

    private val _effects = Channel<ManageMatchEffect>(Channel.BUFFERED)
    val effects: Flow<ManageMatchEffect> = _effects.receiveAsFlow()

    private var currentUserId: String? = null

    private fun ManageMatchState.withCanRatePlayers(): ManageMatchState {
        val game = match ?: return copy(canRatePlayers = false)
        return copy(canRatePlayers = game.canOrganizerRate(userId = currentUserId))
    }

    init {
        loadMatch()
        subscribeToMatch()
        subscribeToCurrentUser()
    }

    fun onEvent(event: ManageMatchEvent) {
        when (event) {
            ManageMatchEvent.Retry -> loadMatch()
            ManageMatchEvent.DismissActionError -> {
                _state.update { it.copy(actionErrorMessage = null) }
            }
            ManageMatchEvent.DismissSuccess -> {
                _state.update { it.copy(successMessage = null) }
            }
            ManageMatchEvent.RequestCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = true) }
            }
            ManageMatchEvent.ConfirmCancelMatch -> cancelMatchAction()
            ManageMatchEvent.CancelCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = false) }
            }
            ManageMatchEvent.RequestCancelSeries -> {
                _state.update { it.copy(showCancelSeriesConfirmDialog = true) }
            }
            ManageMatchEvent.ConfirmCancelSeries -> cancelMatchSeriesAction()
            ManageMatchEvent.CancelCancelSeries -> {
                _state.update { it.copy(showCancelSeriesConfirmDialog = false) }
            }
            is ManageMatchEvent.TeamCountSelected -> {
                _state.update { it.copy(selectedTeamCount = event.teamCount) }
            }
            ManageMatchEvent.ShuffleTeams -> shuffleTeamsAction()
            is ManageMatchEvent.MovePlayerToTeam -> movePlayerToTeamAction(event.userId, event.teamIndex)
            is ManageMatchEvent.OpenRatingSheet -> {
                _state.update {
                    it.copy(
                        showRatingSheet = true,
                        selectedPlayerForRating = event.userId to event.displayName,
                        existingRatingForSelectedPlayer = it.organizerRatingsGiven[event.userId],
                    )
                }
            }
            ManageMatchEvent.CloseRatingSheet -> {
                _state.update {
                    it.copy(showRatingSheet = false, selectedPlayerForRating = null, existingRatingForSelectedPlayer = null)
                }
            }
            is ManageMatchEvent.SubmitRating -> {
                submitPlayerRating(event.rating, event.comment, event.reportReason, event.reportDetails)
            }
            ManageMatchEvent.DismissRatingError -> {
                _state.update { it.copy(ratingErrorMessage = null) }
            }
            ManageMatchEvent.DismissRatingSuccess -> {
                _state.update { it.copy(ratingSuccessMessage = null) }
            }
        }
    }

    private fun loadMatch() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getGameById(matchId)
                .onSuccess { game ->
                    _state.update { it.copy(isLoading = false, match = game).withCanRatePlayers() }
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
                        _state.update { it.copy(match = game, isLoading = false).withCanRatePlayers() }
                    }
                }
        }
    }

    private fun subscribeToCurrentUser() {
        screenModelScope.launch {
            currentUserId = sessionHolder.currentUser.first()?.uid
            _state.update { it.copy(currentUserId = currentUserId).withCanRatePlayers() }
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
                        _state.update { it.copy(confirmedPlayers = summary.confirmed) }
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

    private fun cancelMatchAction() {
        val sport =
            _state.value.match
                ?.sport
                ?.name ?: UNKNOWN_SPORT
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update { it.copy(isCancellingMatch = true, showCancelConfirmDialog = false, actionErrorMessage = null) }

            cancelMatch(matchId)
                .onSuccess {
                    analytics.track(AnalyticsEvent.MatchCancelled(sport, isSeries = false))
                    _state.update { it.copy(isCancellingMatch = false) }
                    _effects.send(ManageMatchEffect.MatchCancelled)
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            actionErrorMessage = strings.cancelError,
                        )
                    }
                }
        }
    }

    private fun cancelMatchSeriesAction() {
        val sport =
            _state.value.match
                ?.sport
                ?.name ?: UNKNOWN_SPORT
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update {
                it.copy(isCancellingSeries = true, showCancelSeriesConfirmDialog = false, actionErrorMessage = null)
            }

            cancelMatchSeries(matchId)
                .onSuccess {
                    analytics.track(AnalyticsEvent.MatchCancelled(sport, isSeries = true))
                    _state.update {
                        it.copy(
                            isCancellingSeries = false,
                            successMessage = strings.cancelSeriesSuccess,
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isCancellingSeries = false,
                            actionErrorMessage = strings.cancelSeriesError,
                        )
                    }
                }
        }
    }

    private fun shuffleTeamsAction() {
        val playerIds = _state.value.confirmedPlayers.map { it.userId }
        val teamCount = _state.value.selectedTeamCount
        val assignments = shuffleIntoTeams(playerIds, teamCount)
        writeTeamAssignments(teamCount, assignments)
    }

    private fun movePlayerToTeamAction(
        userId: String,
        teamIndex: Int,
    ) {
        val match = _state.value.match ?: return
        val updatedAssignments = match.teamAssignments + (userId to teamIndex)
        writeTeamAssignments(match.teamCount, updatedAssignments)
    }

    private fun writeTeamAssignments(
        teamCount: Int,
        assignments: Map<String, Int>,
    ) {
        screenModelScope.launch {
            _state.update { it.copy(isSavingTeams = true, actionErrorMessage = null) }
            setTeamAssignments(matchId, teamCount, assignments)
                .onSuccess {
                    _state.update { it.copy(isSavingTeams = false) }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isSavingTeams = false,
                            actionErrorMessage = stringsHolder.resolveStringsOrDefault().manageMatch.teamsSaveError,
                        )
                    }
                }
        }
    }

    private companion object {
        const val UNKNOWN_SPORT = "unknown"
    }
}
