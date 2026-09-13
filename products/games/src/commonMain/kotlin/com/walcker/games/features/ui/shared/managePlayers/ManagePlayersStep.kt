package com.walcker.games.features.ui.shared.managePlayers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.shared.manageMatch.component.RateablePlayersList
import com.walcker.games.features.ui.shared.managePlayers.component.WaitlistPlayerRow
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.ConfirmDialog
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.features.ui.shared.ratings.RatingBottomSheet
import com.walcker.games.strings.GamesStrings
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class ManagePlayersStep(
    private val matchId: String,
) : Screen {
    override val key: String get() = "manage-players-$matchId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<ManagePlayersStepModel>(
                parameters = { parametersOf(matchId) },
            )
        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings
        val bottomBarVisibility: BottomBarVisibilityCoordinator = koinInject()

        DisposableEffect(bottomBarVisibility) {
            bottomBarVisibility.setVisible(false)
            onDispose { bottomBarVisibility.setVisible(true) }
        }

        ManagePlayersContent(
            state = state,
            strings = strings,
            onEvent = stepModel::onEvent,
            onDismiss = { navigator.pop() },
        )
    }
}

@Composable
internal fun ManagePlayersContent(
    state: ManagePlayersState,
    strings: GamesStrings,
    onEvent: (ManagePlayersEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val manage = strings.manageMatch

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CedarTokens.colors.canvas)
                .navigationBarsPadding(),
    ) {
        CedarTopBar(
            title = manage.managePlayersTitle,
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
                    onDismiss = { onEvent(ManagePlayersEvent.DismissActionError) },
                )
            }
            state.successMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    dismissContentDescription = manage.dismissContentDescription,
                    onDismiss = { onEvent(ManagePlayersEvent.DismissSuccess) },
                )
            }

            when {
                state.isLoading -> LoadingBlock(contentDescription = manage.loadingLabel)

                state.errorMessage != null ->
                    EmptyState(
                        message = state.errorMessage.orEmpty(),
                        actionLabel = manage.retry,
                        onAction = { onEvent(ManagePlayersEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                state.match != null ->
                    ManagePlayersBody(
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

    RatingBottomSheet(
        isVisible = state.showRatingSheet,
        playerName = state.selectedPlayerForRating?.second ?: "",
        strings = strings.ratings,
        reportStrings = strings.reports,
        onDismiss = { onEvent(ManagePlayersEvent.CloseRatingSheet) },
        onSubmit = { rating, comment, reportReason, reportDetails ->
            onEvent(ManagePlayersEvent.SubmitRating(rating, comment, reportReason, reportDetails))
        },
        initialRating = state.existingRatingForSelectedPlayer?.rating ?: 5,
        initialComment = state.existingRatingForSelectedPlayer?.comment ?: "",
        isLoading = state.isSubmittingRating,
    )

    state.playerPendingBan?.let { (_, displayName) ->
        ConfirmDialog(
            title = manage.banConfirmTitle(displayName),
            body = manage.banConfirmMessage,
            confirmLabel = manage.banConfirmButton,
            dismissLabel = manage.banCancelButton,
            isWorking = state.isBanningPlayer,
            onConfirm = { onEvent(ManagePlayersEvent.ConfirmBan) },
            onDismiss = { onEvent(ManagePlayersEvent.CancelBan) },
        )
    }
}

@Composable
private fun ManagePlayersBody(
    state: ManagePlayersState,
    manage: ManageMatchStrings,
    onEvent: (ManagePlayersEvent) -> Unit,
) {
    val showVip = !state.match?.seriesId.isNullOrEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
            Text(
                text = manage.gameListSection,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                        onEvent(ManagePlayersEvent.OpenRatingSheet(userId, displayName))
                    },
                    onBanPlayer = { userId, displayName ->
                        onEvent(ManagePlayersEvent.RequestBan(userId, displayName))
                    },
                    showVip = showVip,
                    onToggleVip = { userId, displayName, currentlyVip ->
                        onEvent(ManagePlayersEvent.ToggleVip(userId, displayName, currentlyVip))
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
            Text(
                text = manage.waitlistSection,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.waitlistPlayers.isEmpty()) {
                Text(
                    text = manage.waitlistEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                    state.waitlistPlayers.forEach { participant ->
                        WaitlistPlayerRow(
                            participant = participant,
                            positionLabel = manage.waitlistPositionLabel(participant.positionInWaitlist ?: 0),
                            confirmLabel = manage.confirmWaitlistedAction,
                            banLabel = manage.banAction,
                            onConfirmToGame = { userId, displayName ->
                                onEvent(ManagePlayersEvent.ConfirmWaitlisted(userId, displayName))
                            },
                            onBanPlayer = { userId, displayName ->
                                onEvent(ManagePlayersEvent.RequestBan(userId, displayName))
                            },
                            showVip = showVip,
                            vipLabel = if (participant.isVip) manage.vipRemoveAction else manage.vipAction,
                            onToggleVip = { userId, displayName, currentlyVip ->
                                onEvent(ManagePlayersEvent.ToggleVip(userId, displayName, currentlyVip))
                            },
                        )
                    }
                }
            }
        }
    }
}
