package com.walcker.games.features.ui.shared.teamShuffle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.ui.shared.manageMatch.component.TeamShuffleSection
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class TeamShuffleStep(
    private val matchId: String,
) : Screen {
    override val key: String get() = "team-shuffle-$matchId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<TeamShuffleStepModel>(
                parameters = { parametersOf(matchId) },
            )
        val state by stepModel.state.collectAsState()
        val manage = rememberGamesStrings().strings.manageMatch
        val bottomBarVisibility: BottomBarVisibilityCoordinator = koinInject()

        DisposableEffect(bottomBarVisibility) {
            bottomBarVisibility.setVisible(false)
            onDispose { bottomBarVisibility.setVisible(true) }
        }

        TeamShuffleContent(
            state = state,
            manage = manage,
            onEvent = stepModel::onEvent,
            onDismiss = { navigator.pop() },
        )
    }
}

@Composable
internal fun TeamShuffleContent(
    state: TeamShuffleState,
    manage: ManageMatchStrings,
    onEvent: (TeamShuffleEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CedarTokens.colors.canvas)
                .navigationBarsPadding(),
    ) {
        CedarTopBar(
            title = manage.teamsSectionTitle,
            onBack = onDismiss,
            backContentDescription = manage.backContentDescription,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.actionErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(TeamShuffleEvent.DismissActionError) },
                )
            }

            when {
                state.isLoading -> LoadingBlock(contentDescription = manage.loadingLabel)

                state.errorMessage != null ->
                    EmptyState(
                        message = state.errorMessage.orEmpty(),
                        actionLabel = manage.retry,
                        onAction = { onEvent(TeamShuffleEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                state.match != null ->
                    TeamShuffleBody(
                        match = state.match,
                        state = state,
                        manage = manage,
                        onEvent = onEvent,
                    )

                else ->
                    EmptyState(
                        message = manage.notFound,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
        }
    }
}

@Composable
private fun TeamShuffleBody(
    match: Game,
    state: TeamShuffleState,
    manage: ManageMatchStrings,
    onEvent: (TeamShuffleEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.md),
    ) {
        TeamShuffleSection(
            selectedTeamCount = state.selectedTeamCount,
            selectedPlayersPerTeam = state.selectedPlayersPerTeam,
            teamCount = match.teamCount,
            playersPerTeam = match.playersPerTeam,
            teamAssignments = match.teamAssignments,
            confirmedPlayers = state.confirmedPlayers,
            skillRatings = state.skillRatings,
            mySkillRatings = state.mySkillRatings,
            canRateSkill = state.canRateSkill,
            strings = manage,
            isSaving = state.isSavingTeams,
            onTeamCountSelected = { count -> onEvent(TeamShuffleEvent.TeamCountSelected(count)) },
            onPlayersPerTeamSelected = { count -> onEvent(TeamShuffleEvent.PlayersPerTeamSelected(count)) },
            onShuffle = { onEvent(TeamShuffleEvent.ShuffleTeams) },
            onMovePlayer = { userId, teamIndex -> onEvent(TeamShuffleEvent.MovePlayerToTeam(userId, teamIndex)) },
            onRateSkill = { userId, rating -> onEvent(TeamShuffleEvent.RateSkill(userId, rating)) },
        )
    }
}
