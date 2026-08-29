package com.walcker.games.features.ui.matchdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.model.Participant
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.JoinGameUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.usecase.SubmitReportUseCase
import com.walcker.games.features.ui.common.LoginRequiredBottomSheet
import com.walcker.games.features.ui.ratings.RatingBottomSheet
import com.walcker.games.features.ui.reports.ReportBottomSheet
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.games.strings.ReportStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.identity.api.SessionHolder
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarAvailabilityButton
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.datetime.formatWhen
import com.walcker.match.navigator.LoginCoordinator
import com.walcker.match.navigator.PromotionCoordinator
import org.koin.compose.koinInject

internal class MatchDetailStep(val matchId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val getGameById: GetGameByIdUseCase = koinInject()
        val observeMatch: ObserveMatchUseCase = koinInject()
        val observeParticipants: ObserveParticipantsUseCase = koinInject()
        val submitRating: SubmitRatingUseCase = koinInject()
        val submitReport: SubmitReportUseCase = koinInject()
        val sessionHolder: SessionHolder = koinInject()
        val promotionCoordinator: PromotionCoordinator = koinInject()
        val stringsHolder: GamesStringsHolder = koinInject()
        val analytics: AnalyticsTracker = koinInject()
        val loginCoordinator: LoginCoordinator = koinInject()

        val joinGame: JoinGameUseCase = koinInject()
        val leaveGame: LeaveMatchUseCase = koinInject()
        val cancelGame: CancelMatchUseCase = koinInject()

        val stepModel = remember {
            MatchDetailStepModel(
                getGameById = getGameById,
                observeMatch = observeMatch,
                observeParticipants = observeParticipants,
                joinGame = joinGame,
                leaveMatch = leaveGame,
                cancelMatch = cancelGame,
                submitRating = submitRating,
                submitReport = submitReport,
                sessionHolder = sessionHolder,
                promotionCoordinator = promotionCoordinator,
                stringsHolder = stringsHolder,
                analytics = analytics,
                matchId = matchId,
            )
        }

        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings
        val detail = strings.matchDetail
        val loginRequired = strings.loginRequired
        var showLoginSheet by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is MatchDetailEffect.NavigateToConfirmation ->
                        navigator.replace(
                            MatchConfirmedStep(
                                matchId = effect.matchId,
                                venueName = effect.venueName,
                                startsAtSeconds = effect.startsAtSeconds,
                                sportLabel = effect.sportLabel,
                            ),
                        )

                    MatchDetailEffect.RequireLogin -> showLoginSheet = true
                }
            }
        }

        val match = state.match
        val confirmed = state.participants?.confirmedCount ?: match?.confirmedPlayers ?: 0
        val total = state.participants?.totalSlots ?: match?.totalPlayers ?: 0
        val openSlots = (total - confirmed).coerceAtLeast(0)
        val isFull = match != null && (confirmed >= total || match.status == MatchStatus.FULL)
        val isClosed = match != null && (
            state.isMatchOver ||
                match.status == MatchStatus.FINISHED ||
                match.status == MatchStatus.CANCELLED
            )

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            topBar = {
                CedarTopBar(
                    title = detail.title,
                    onBack = { navigator.pop() },
                    backContentDescription = detail.backContentDescription,
                )
            },
            bottomBar = {
                if (match != null) {
                    JoinBar(
                        label = when {
                            isClosed -> detail.matchClosed
                            isFull -> detail.joinWaitlist
                            else -> detail.joinMatch
                        },
                        priceLabel = match.pricePerPlayer,
                        enabled = !isClosed && !state.isJoining,
                        isLoading = state.isJoining,
                        useAvailabilityTone = !isClosed && !isFull,
                        onClick = { stepModel.onEvent(MatchDetailEvent.JoinMatch) },
                    )
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.justPromoted) {
                    Banner(
                        message = detail.promotedFromWaitlist,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                        dismissContentDescription = detail.dismissContentDescription,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissPromotion) },
                    )
                }
                state.successMessage?.let { message ->
                    Banner(
                        message = message,
                        container = CedarTokens.colors.availableContainer,
                        onContainer = CedarTokens.colors.availableText,
                        dismissContentDescription = detail.dismissContentDescription,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissSuccess) },
                    )
                }
                state.statusChangeMessage?.let { message ->
                    Banner(
                        message = message,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        dismissContentDescription = detail.dismissContentDescription,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissStatusChange) },
                    )
                }
                state.ratingErrorMessage?.let { message ->
                    Banner(
                        message = message,
                        container = MaterialTheme.colorScheme.errorContainer,
                        onContainer = MaterialTheme.colorScheme.onErrorContainer,
                        dismissContentDescription = detail.dismissContentDescription,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissRatingError) },
                    )
                }
                state.reportErrorMessage?.let { message ->
                    Banner(
                        message = message,
                        container = MaterialTheme.colorScheme.errorContainer,
                        onContainer = MaterialTheme.colorScheme.onErrorContainer,
                        dismissContentDescription = detail.dismissContentDescription,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissReportError) },
                    )
                }

                when {
                    state.isLoading -> LoadingBlock(contentDescription = detail.loadingLabel)

                    state.errorMessage != null -> EmptyState(
                        message = state.errorMessage.orEmpty(),
                        actionLabel = detail.retry,
                        onAction = { stepModel.onEvent(MatchDetailEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    match != null -> MatchDetailContent(
                        match = match,
                        participants = state.participants,
                        detail = detail,
                        confirmed = confirmed,
                        total = total,
                        openSlots = openSlots,
                        isClosed = isClosed,
                        canRate = state.canRate,
                        isMatchOver = state.isMatchOver,
                        isLeavingMatch = state.isLeavingMatch,
                        isCancellingMatch = state.isCancellingMatch,
                        currentUserId = state.currentUserId,
                        reportStrings = strings.reports,
                        onReportPlayer = { userId, displayName ->
                            stepModel.onEvent(MatchDetailEvent.OpenReportSheet(userId, displayName))
                        },
                        onRatePlayer = { userId, displayName ->
                            stepModel.onEvent(MatchDetailEvent.OpenRatingSheet(userId, displayName))
                        },
                        onLeaveMatch = { stepModel.onEvent(MatchDetailEvent.RequestLeaveMatch) },
                        onCancelMatch = { stepModel.onEvent(MatchDetailEvent.RequestCancelMatch) },
                    )

                    else -> EmptyState(
                        message = detail.notFound,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        ReportBottomSheet(
            isVisible = state.showReportSheet,
            playerName = state.selectedPlayerForReport?.second ?: "",
            strings = strings.reports,
            isSubmitting = state.isSubmittingReport,
            onDismiss = { stepModel.onEvent(MatchDetailEvent.CloseReportSheet) },
            onSubmit = { reason, details ->
                stepModel.onEvent(MatchDetailEvent.SubmitReport(reason, details))
            },
        )

        RatingBottomSheet(
            isVisible = state.showRatingSheet,
            playerName = state.selectedPlayerForRating?.second ?: "",
            strings = strings.ratings,
            onDismiss = { stepModel.onEvent(MatchDetailEvent.CloseRatingSheet) },
            onSubmit = { rating, comment, dimensions ->
                stepModel.onEvent(MatchDetailEvent.SubmitRating(rating, comment, dimensions))
            },
            isLoading = state.isSubmittingRating,
        )

        if (state.showLeaveConfirmDialog) {
            ConfirmDialog(
                title = detail.leaveDialogTitle,
                body = detail.leaveDialogBody,
                confirmLabel = detail.leaveDialogConfirm,
                dismissLabel = detail.dialogDismiss,
                isWorking = state.isLeavingMatch,
                onConfirm = { stepModel.onEvent(MatchDetailEvent.ConfirmLeaveMatch) },
                onDismiss = { stepModel.onEvent(MatchDetailEvent.CancelLeaveMatch) },
            )
        }

        if (state.showCancelConfirmDialog) {
            ConfirmDialog(
                title = detail.cancelDialogTitle,
                body = detail.cancelDialogBody,
                confirmLabel = detail.cancelDialogConfirm,
                dismissLabel = detail.dialogDismiss,
                isWorking = state.isCancellingMatch,
                onConfirm = { stepModel.onEvent(MatchDetailEvent.ConfirmCancelMatch) },
                onDismiss = { stepModel.onEvent(MatchDetailEvent.CancelCancelMatch) },
            )
        }

        LoginRequiredBottomSheet(
            isVisible = showLoginSheet,
            strings = loginRequired,
            onConfirm = {
                loginCoordinator.requestLogin()
                showLoginSheet = false
            },
            onDismiss = { showLoginSheet = false },
        )
    }
}

@Composable
private fun JoinBar(
    label: String,
    priceLabel: String?,
    enabled: Boolean,
    isLoading: Boolean,
    useAvailabilityTone: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = CedarTokens.elevation.overlay,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.sm,
            ),
        ) {
            val text = if (priceLabel != null && enabled) "$label · $priceLabel" else label
            if (useAvailabilityTone) {
                CedarAvailabilityButton(
                    text = text,
                    onClick = onClick,
                    enabled = enabled,
                    loading = isLoading,
                )
            } else {
                CedarPrimaryButton(
                    text = text,
                    onClick = onClick,
                    enabled = enabled,
                    loading = isLoading,
                )
            }
        }
    }
}

@Composable
private fun MatchDetailContent(
    match: Game,
    participants: ParticipantsSummary?,
    detail: MatchDetailStrings,
    confirmed: Int,
    total: Int,
    openSlots: Int,
    isClosed: Boolean,
    canRate: Boolean,
    isMatchOver: Boolean,
    currentUserId: String?,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    onLeaveMatch: () -> Unit,
    onCancelMatch: () -> Unit,
    isLeavingMatch: Boolean,
    isCancellingMatch: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
            Text(
                text = "${match.sport.label} · ${match.neighborhood}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = match.venueName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatWhen(startsAtSeconds = match.startsAtSeconds),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = match.pricePerPlayer ?: detail.freePrice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StatusBadge(status = match.status, isMatchOver = isMatchOver, detail = detail)

        Card(
            shape = CedarTokens.radius.lgShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(CedarTokens.spacing.md),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Text(
                    text = detail.confirmedOf(confirmed, total),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (openSlots > 0) {
                        detail.openSlotsRemaining(openSlots)
                    } else {
                        detail.noSlotsRemaining
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (openSlots > 0) {
                        CedarTokens.colors.availableText
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xl)) {
            LabelledValue(
                label = detail.durationLabel,
                value = detail.durationValue(match.durationMin),
            )
            LabelledValue(
                label = detail.priceLabel,
                value = match.pricePerPlayer ?: detail.freePrice,
            )
        }

        Text(
            text = detail.organizer(match.organizerName, match.organizerRating),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CedarSectionHeader(title = detail.participants)

        if (participants != null) {
            ParticipantsList(
                participants = participants,
                detail = detail,
                canRate = canRate,
                currentUserId = currentUserId,
                reportStrings = reportStrings,
                onReportPlayer = onReportPlayer,
                onRatePlayer = onRatePlayer,
            )
        } else {
            StaticParticipantsList(
                participantIds = match.participants,
                organizerName = match.organizerName,
                detail = detail,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            CedarSecondaryButton(
                text = detail.leaveMatch,
                onClick = onLeaveMatch,
                enabled = !isClosed && !isLeavingMatch && !isCancellingMatch,
                loading = isLeavingMatch,
                fillWidth = false,
                modifier = Modifier.weight(1f),
            )
            CedarSecondaryButton(
                text = detail.cancelMatch,
                onClick = onCancelMatch,
                enabled = !isClosed && !isCancellingMatch && !isLeavingMatch,
                loading = isCancellingMatch,
                fillWidth = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusBadge(
    status: MatchStatus,
    isMatchOver: Boolean,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when {
        status == MatchStatus.CANCELLED ->
            detail.statusCancelled to MaterialTheme.colorScheme.error
        status == MatchStatus.FINISHED || isMatchOver ->
            detail.statusFinished to MaterialTheme.colorScheme.onSurfaceVariant
        status == MatchStatus.FULL ->
            detail.statusFull to MaterialTheme.colorScheme.error
        else ->
            detail.statusOpen to MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = BADGE_TINT), shape = CedarTokens.radius.pill)
            .padding(
                horizontal = CedarTokens.spacing.sm,
                vertical = CedarTokens.spacing.xxs,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private const val BADGE_TINT = 0.15f

@Composable
private fun ParticipantsList(
    participants: ParticipantsSummary,
    detail: MatchDetailStrings,
    canRate: Boolean,
    currentUserId: String?,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        if (participants.confirmed.isNotEmpty()) {
            Text(
                text = detail.confirmedSection(participants.confirmed.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            participants.confirmed.forEach { participant ->
                ParticipantRow(
                    participant = participant,
                    statusLabel = detail.confirmedTag,
                    paidLabel = detail.paidTag,
                    rateLabel = detail.rateAction,
                    canRate = canRate,
                    canReport = participant.userId != currentUserId,
                    reportStrings = reportStrings,
                    onReportPlayer = onReportPlayer,
                    onRatePlayer = onRatePlayer,
                )
            }
        }

        if (participants.waitlist.isNotEmpty()) {
            Text(
                text = detail.waitlistSection(participants.waitlist.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CedarTokens.spacing.xs),
            )
            participants.waitlist.forEach { participant ->
                ParticipantRow(
                    participant = participant,
                    statusLabel = detail.queuePosition(participant.positionInWaitlist ?: 0),
                    paidLabel = detail.paidTag,
                    rateLabel = detail.rateAction,
                    canRate = false,
                    canReport = participant.userId != currentUserId,
                    reportStrings = reportStrings,
                    onReportPlayer = onReportPlayer,
                    onRatePlayer = onRatePlayer,
                )
            }
        }
    }
}

@Composable
private fun StaticParticipantsList(
    participantIds: List<String>,
    organizerName: String,
    detail: MatchDetailStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        participantIds.forEachIndexed { index, _ ->
            Text(
                text = if (index == 0) organizerName else detail.anonymousPlayer(index + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant,
    statusLabel: String,
    paidLabel: String,
    rateLabel: String,
    canRate: Boolean,
    canReport: Boolean,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CedarTokens.radius.smShape,
            )
            .padding(
                horizontal = CedarTokens.spacing.sm,
                vertical = CedarTokens.spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(
            displayName = participant.displayName,
            photoUrl = participant.photoUrl,
            size = PlayerAvatarSize.Small,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (participant.hasPaid) "$statusLabel · $paidLabel" else statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (canRate) {
            TextButton(
                onClick = { onRatePlayer(participant.userId, participant.displayName) },
            ) {
                Text(text = rateLabel, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (canReport) {
            IconButton(
                onClick = { onReportPlayer(participant.userId, participant.displayName) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = reportStrings.reportAction,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Banner(
    message: String,
    container: Color,
    onContainer: Color,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.xs,
            )
            .background(color = container, shape = CedarTokens.radius.smShape)
            .padding(
                start = CedarTokens.spacing.md,
                top = CedarTokens.spacing.xs,
                bottom = CedarTokens.spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = onContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = dismissContentDescription,
                tint = onContainer,
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        shape = CedarTokens.radius.lgShape,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(DIALOG_SPINNER),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

private val DIALOG_SPINNER = 16.dp

@Composable
private fun LoadingBlock(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(CedarTokens.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        CedarLoading(contentDescription = contentDescription)
    }
}
