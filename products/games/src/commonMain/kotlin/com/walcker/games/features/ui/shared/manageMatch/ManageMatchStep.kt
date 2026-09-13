package com.walcker.games.features.ui.shared.manageMatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.isInProgress
import com.walcker.games.features.domain.shared.model.supportsTeamShuffle
import com.walcker.games.features.ui.create.CreateMatchStep
import com.walcker.games.features.ui.shared.manageMatch.component.MatchActionCard
import com.walcker.games.features.ui.shared.manageMatch.component.MatchHeaderCard
import com.walcker.games.features.ui.shared.managePlayers.ManagePlayersStep
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.ConfirmDialog
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.features.ui.shared.teamShuffle.TeamShuffleStep
import com.walcker.games.strings.GamesStrings
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarTag
import com.walcker.match.cedar.components.CedarTagTone
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

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
            onManageTeams = { navigator.push(TeamShuffleStep(matchId)) },
            onManagePlayers = { navigator.push(ManagePlayersStep(matchId)) },
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
    onManageTeams: () -> Unit = {},
    onManagePlayers: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val manage = strings.manageMatch
    val match = state.match
    val nowSeconds = remember { kotlin.time.Clock.System.now().epochSeconds }

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
            actions = {
                if (match != null) {
                    val (label, tone) = match.statusLabelAndTone(nowSeconds, manage)
                    CedarTag(
                        label = label.uppercase(),
                        tone = tone,
                        modifier = Modifier.padding(end = CedarTokens.spacing.md),
                    )
                }
            },
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
                        onManageTeams = onManageTeams,
                        onManagePlayers = onManagePlayers,
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
}

private fun Game.statusLabelAndTone(
    nowSeconds: Long,
    manage: ManageMatchStrings,
): Pair<String, CedarTagTone> =
    when {
        status == MatchStatus.CANCELLED -> manage.statusCancelled to CedarTagTone.Danger
        status == MatchStatus.FINISHED || isOver(nowSeconds) -> manage.statusFinished to CedarTagTone.Neutral
        isInProgress(nowSeconds) -> manage.statusInProgress to CedarTagTone.Available
        status == MatchStatus.FULL -> manage.statusFull to CedarTagTone.Warning
        else -> manage.statusOpen to CedarTagTone.Info
    }

@Composable
private fun ManageMatchBody(
    match: Game,
    state: ManageMatchState,
    manage: ManageMatchStrings,
    onEvent: (ManageMatchEvent) -> Unit,
    onEditMatch: () -> Unit,
    onManageTeams: () -> Unit,
    onManagePlayers: () -> Unit,
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
        MatchHeaderCard(
            match = match,
            vipCount = state.vipCount,
            manage = manage,
            onEditMatch = onEditMatch,
            canEdit = !isClosed,
        )

        val mainActions =
            buildList {
                if (match.supportsTeamShuffle()) {
                    add(
                        MatchAction(
                            label = manage.teamsSectionTitle,
                            icon = Icons.Filled.Shuffle,
                            onClick = onManageTeams,
                        ),
                    )
                }
                add(
                    MatchAction(
                        label = manage.managePlayersAction,
                        icon = Icons.Filled.Groups,
                        onClick = onManagePlayers,
                    ),
                )
            }

        val cancelActions =
            buildList {
                add(
                    MatchAction(
                        label = manage.cancelMatch,
                        icon = Icons.Filled.EventBusy,
                        onClick = { onEvent(ManageMatchEvent.RequestCancelMatch) },
                        enabled = !isClosed,
                        isWorking = state.isCancellingMatch,
                        isDanger = true,
                    ),
                )
                if (match.recurrence != RecurrenceOption.NONE) {
                    add(
                        MatchAction(
                            label = manage.cancelMatchSeries,
                            icon = Icons.Filled.EventRepeat,
                            onClick = { onEvent(ManageMatchEvent.RequestCancelSeries) },
                            enabled = !isClosed,
                            isWorking = state.isCancellingSeries,
                            isDanger = true,
                        ),
                    )
                }
            }

        ActionCardRows(actions = mainActions, isCompact = false)
        ActionCardRows(actions = cancelActions, isCompact = true)
    }
}

@Composable
private fun ActionCardRows(
    actions: List<MatchAction>,
    isCompact: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                rowActions.forEach { action ->
                    MatchActionCard(
                        label = action.label,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                        enabled = action.enabled,
                        isWorking = action.isWorking,
                        isDanger = action.isDanger,
                        isCompact = isCompact,
                    )
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MatchAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isWorking: Boolean = false,
    val isDanger: Boolean = false,
)
