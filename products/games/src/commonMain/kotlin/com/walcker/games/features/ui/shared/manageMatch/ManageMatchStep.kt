package com.walcker.games.features.ui.shared.manageMatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.supportsTeamShuffle
import com.walcker.games.features.ui.create.CreateMatchStep
import com.walcker.games.features.ui.shared.manageMatch.component.RateablePlayersList
import com.walcker.games.features.ui.shared.manageMatch.component.TeamShuffleSection
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.ConfirmDialog
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.features.ui.shared.ratings.RatingBottomSheet
import com.walcker.games.strings.GamesStrings
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private val ActionLoadingSize = 28.dp

internal class ManageMatchStep(
    private val matchId: String,
) : Screen {
    override val key: String get() = "manage-match-$matchId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<ManageMatchStepModel>(
                parameters = { parametersOf(matchId) },
            )
        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings
        val bottomBarVisibility: BottomBarVisibilityCoordinator = koinInject()

        DisposableEffect(bottomBarVisibility) {
            bottomBarVisibility.setVisible(false)
            onDispose { bottomBarVisibility.setVisible(true) }
        }

        LaunchedEffect(stepModel) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    ManageMatchEffect.MatchCancelled -> navigator.pop()
                }
            }
        }

        ManageMatchContent(
            state = state,
            strings = strings,
            onEvent = stepModel::onEvent,
            onEditMatch = { navigator.push(CreateMatchStep(matchId)) },
            onDismiss = { navigator.pop() },
        )
    }
}

@Composable
internal fun ManageMatchContent(
    state: ManageMatchState,
    strings: GamesStrings,
    onEvent: (ManageMatchEvent) -> Unit,
    modifier: Modifier = Modifier,
    onEditMatch: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val manage = strings.manageMatch
    val match = state.match

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CedarTokens.colors.canvas),
    ) {
        CedarTopBar(
            title = manage.title,
            onBack = onDismiss,
            backContentDescription = manage.backContentDescription,
            leadingIcon = Icons.Default.Close,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.successMessage?.let { message ->
                Banner(
                    message = message,
                    container = CedarTokens.colors.availableContainer,
                    onContainer = CedarTokens.colors.availableText,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(ManageMatchEvent.DismissSuccess) },
                )
            }
            state.actionErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(ManageMatchEvent.DismissActionError) },
                )
            }
            state.ratingSuccessMessage?.let { message ->
                Banner(
                    message = message,
                    container = CedarTokens.colors.availableContainer,
                    onContainer = CedarTokens.colors.availableText,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(ManageMatchEvent.DismissRatingSuccess) },
                )
            }
            state.ratingErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(ManageMatchEvent.DismissRatingError) },
                )
            }

            when {
                state.isLoading -> LoadingBlock(contentDescription = manage.loadingLabel)

                state.errorMessage != null ->
                    EmptyState(
                        message = state.errorMessage.orEmpty(),
                        actionLabel = manage.retry,
                        onAction = { onEvent(ManageMatchEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                match != null ->
                    ManageMatchBody(
                        match = match,
                        state = state,
                        manage = manage,
                        onEvent = onEvent,
                        onEditMatch = onEditMatch,
                    )

                else ->
                    EmptyState(
                        message = manage.notFound,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
        }
    }

    if (state.showCancelConfirmDialog) {
        ConfirmDialog(
            title = manage.cancelDialogTitle,
            body = manage.cancelDialogBody,
            confirmLabel = manage.cancelDialogConfirm,
            dismissLabel = manage.dialogDismiss,
            isWorking = state.isCancellingMatch,
            onConfirm = { onEvent(ManageMatchEvent.ConfirmCancelMatch) },
            onDismiss = { onEvent(ManageMatchEvent.CancelCancelMatch) },
        )
    }

    if (state.showCancelSeriesConfirmDialog) {
        ConfirmDialog(
            title = manage.cancelSeriesDialogTitle,
            body = manage.cancelSeriesDialogBody,
            confirmLabel = manage.cancelSeriesDialogConfirm,
            dismissLabel = manage.dialogDismiss,
            isWorking = state.isCancellingSeries,
            onConfirm = { onEvent(ManageMatchEvent.ConfirmCancelSeries) },
            onDismiss = { onEvent(ManageMatchEvent.CancelCancelSeries) },
        )
    }

    RatingBottomSheet(
        isVisible = state.showRatingSheet,
        playerName = state.selectedPlayerForRating?.second ?: "",
        strings = strings.ratings,
        reportStrings = strings.reports,
        onDismiss = { onEvent(ManageMatchEvent.CloseRatingSheet) },
        onSubmit = { rating, comment, reportReason, reportDetails ->
            onEvent(ManageMatchEvent.SubmitRating(rating, comment, reportReason, reportDetails))
        },
        initialRating = state.existingRatingForSelectedPlayer?.rating ?: 5,
        initialComment = state.existingRatingForSelectedPlayer?.comment ?: "",
        isLoading = state.isSubmittingRating,
    )
}

@Composable
private fun ManageMatchBody(
    match: Game,
    state: ManageMatchState,
    manage: ManageMatchStrings,
    onEvent: (ManageMatchEvent) -> Unit,
    onEditMatch: () -> Unit,
) {
    val isClosed = match.status == MatchStatus.FINISHED || match.status == MatchStatus.CANCELLED

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        Text(
            text = match.venueName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        CedarSecondaryButton(
            text = manage.editMatch,
            onClick = onEditMatch,
            enabled = !isClosed,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isCancellingMatch) {
            CedarLoading(contentDescription = manage.cancelMatch, size = ActionLoadingSize)
        } else {
            CedarSecondaryButton(
                text = manage.cancelMatch,
                onClick = { onEvent(ManageMatchEvent.RequestCancelMatch) },
                enabled = !isClosed,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (match.recurrence != RecurrenceOption.NONE) {
            if (state.isCancellingSeries) {
                CedarLoading(contentDescription = manage.cancelMatchSeries, size = ActionLoadingSize)
            } else {
                CedarTextButton(
                    text = manage.cancelMatchSeries,
                    onClick = { onEvent(ManageMatchEvent.RequestCancelSeries) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (match.supportsTeamShuffle()) {
            TeamShuffleSection(
                selectedTeamCount = state.selectedTeamCount,
                teamCount = match.teamCount,
                teamAssignments = match.teamAssignments,
                confirmedPlayers = state.confirmedPlayers,
                strings = manage,
                isSaving = state.isSavingTeams,
                onTeamCountSelected = { count -> onEvent(ManageMatchEvent.TeamCountSelected(count)) },
                onShuffle = { onEvent(ManageMatchEvent.ShuffleTeams) },
                onMovePlayer = { userId, teamIndex -> onEvent(ManageMatchEvent.MovePlayerToTeam(userId, teamIndex)) },
            )
        }

        if (state.canRatePlayers) {
            CedarSectionHeader(title = manage.ratePlayersSection)
            if (state.confirmedPlayers.isEmpty()) {
                Text(
                    text = manage.ratePlayersEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RateablePlayersList(
                    confirmedPlayers = state.confirmedPlayers,
                    strings = manage,
                    organizerRatingsGiven = state.organizerRatingsGiven,
                    currentUserId = state.currentUserId,
                    participantRatings = state.participantRatings,
                    onRatePlayer = { userId, displayName ->
                        onEvent(ManageMatchEvent.OpenRatingSheet(userId, displayName))
                    },
                )
            }
        }
    }
}
