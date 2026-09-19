package com.walcker.games.features.ui.shared.teamShuffle

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.canOrganizerRate
import com.walcker.games.features.domain.shared.repository.PlayerRepository
import com.walcker.games.features.domain.shared.repository.RatingRepository
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.SetTeamAssignmentsUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitSkillRatingUseCase
import com.walcker.games.features.domain.shared.util.shuffleIntoTeams
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val DEFAULT_TEAM_COUNT = 2
internal const val DEFAULT_PLAYERS_PER_TEAM = 5

internal data class TeamShuffleState(
    val match: Game? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val currentUserId: String? = null,
    val canRateSkill: Boolean = false,
    val confirmedPlayers: List<Participant> = emptyList(),
    val skillRatings: Map<String, PlayerRatingSummary> = emptyMap(),
    val mySkillRatings: Map<String, Int> = emptyMap(),
    val selectedTeamCount: Int = DEFAULT_TEAM_COUNT,
    val selectedPlayersPerTeam: Int = DEFAULT_PLAYERS_PER_TEAM,
    val isSavingTeams: Boolean = false,
)

internal sealed interface TeamShuffleEvent {
    data object Retry : TeamShuffleEvent

    data object DismissActionError : TeamShuffleEvent

    data class TeamCountSelected(
        val teamCount: Int,
    ) : TeamShuffleEvent

    data class PlayersPerTeamSelected(
        val playersPerTeam: Int,
    ) : TeamShuffleEvent

    data object ShuffleTeams : TeamShuffleEvent

    data class MovePlayerToTeam(
        val userId: String,
        val teamIndex: Int,
    ) : TeamShuffleEvent

    data class RateSkill(
        val userId: String,
        val rating: Int,
    ) : TeamShuffleEvent
}

internal class TeamShuffleStepModel(
    private val matchId: String,
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val setTeamAssignments: SetTeamAssignmentsUseCase,
    private val submitSkillRating: SubmitSkillRatingUseCase,
    private val playerRepository: PlayerRepository,
    private val ratingRepository: RatingRepository,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(TeamShuffleState())
    val state: StateFlow<TeamShuffleState> = _state.asStateFlow()

    private var currentUserId: String? = null

    private fun TeamShuffleState.withSelectionsFromMatch(): TeamShuffleState {
        val game = match ?: return this
        return copy(
            selectedTeamCount = if (game.teamCount > 0) game.teamCount else selectedTeamCount,
            selectedPlayersPerTeam = if (game.playersPerTeam > 0) game.playersPerTeam else selectedPlayersPerTeam,
        )
    }

    private fun TeamShuffleState.withCanRateSkill(): TeamShuffleState {
        val game = match ?: return copy(canRateSkill = false)
        return copy(canRateSkill = game.canOrganizerRate(userId = currentUserId))
    }

    init {
        loadMatch()
        subscribeToMatch()
        subscribeToCurrentUser()
    }

    fun onEvent(event: TeamShuffleEvent) {
        when (event) {
            TeamShuffleEvent.Retry -> loadMatch()
            TeamShuffleEvent.DismissActionError -> {
                _state.update { it.copy(actionErrorMessage = null) }
            }
            is TeamShuffleEvent.TeamCountSelected -> {
                _state.update { it.copy(selectedTeamCount = event.teamCount) }
            }
            is TeamShuffleEvent.PlayersPerTeamSelected -> {
                _state.update { it.copy(selectedPlayersPerTeam = event.playersPerTeam) }
            }
            TeamShuffleEvent.ShuffleTeams -> shuffleTeamsAction()
            is TeamShuffleEvent.MovePlayerToTeam -> movePlayerToTeamAction(event.userId, event.teamIndex)
            is TeamShuffleEvent.RateSkill -> rateSkillAction(event.userId, event.rating)
        }
    }

    private fun loadMatch() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getGameById(matchId)
                .onSuccess { game ->
                    _state.update { it.copy(isLoading = false, match = game).withSelectionsFromMatch().withCanRateSkill() }
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
                        _state.update { it.copy(match = game, isLoading = false).withSelectionsFromMatch().withCanRateSkill() }
                    }
                }
        }
    }

    private fun subscribeToCurrentUser() {
        screenModelScope.launch {
            currentUserId = sessionHolder.currentUser.first()?.uid
            _state.update { it.copy(currentUserId = currentUserId).withCanRateSkill() }
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
                        loadSkillRatings(summary.confirmed.map { it.userId })
                    }
                }
        }
    }

    private fun loadSkillRatings(userIds: List<String>) {
        if (userIds.isEmpty()) return
        screenModelScope.launch {
            playerRepository.getPlayersSkillRatingSummary(userIds).onSuccess { ratings ->
                _state.update { it.copy(skillRatings = ratings) }
            }
        }
        val organizerId = currentUserId ?: return
        screenModelScope.launch {
            ratingRepository.getMySkillRatings(organizerId, userIds).onSuccess { ratings ->
                _state.update { it.copy(mySkillRatings = ratings) }
            }
        }
    }

    private fun rateSkillAction(
        userId: String,
        rating: Int,
    ) {
        screenModelScope.launch {
            _state.update { it.copy(actionErrorMessage = null) }
            submitSkillRating(matchId, userId, rating)
                .onSuccess { outcome ->
                    _state.update {
                        it.copy(
                            skillRatings =
                                it.skillRatings +
                                    (userId to PlayerRatingSummary(outcome.averageRating, outcome.ratingCount)),
                            mySkillRatings = it.mySkillRatings + (userId to rating),
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(actionErrorMessage = stringsHolder.resolveStringsOrDefault().manageMatch.skillRatingError)
                    }
                }
        }
    }

    private fun shuffleTeamsAction() {
        val playerIds = _state.value.confirmedPlayers.map { it.userId }
        val teamCount = _state.value.selectedTeamCount
        val playersPerTeam = _state.value.selectedPlayersPerTeam
        val skillRatings = _state.value.skillRatings.mapValues { (_, summary) -> summary.rating }
        val assignments = shuffleIntoTeams(playerIds, teamCount, playersPerTeam, skillRatings)
        writeTeamAssignments(teamCount, playersPerTeam, assignments)
    }

    private fun movePlayerToTeamAction(
        userId: String,
        teamIndex: Int,
    ) {
        val match = _state.value.match ?: return
        val updatedAssignments = match.teamAssignments + (userId to teamIndex)
        writeTeamAssignments(match.teamCount, match.playersPerTeam, updatedAssignments)
    }

    private fun writeTeamAssignments(
        teamCount: Int,
        playersPerTeam: Int,
        assignments: Map<String, Int>,
    ) {
        screenModelScope.launch {
            _state.update { it.copy(isSavingTeams = true, actionErrorMessage = null) }
            setTeamAssignments(matchId, teamCount, playersPerTeam, assignments)
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
}
