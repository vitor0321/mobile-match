package com.walcker.games.features.ui.shared.matchDetail

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.PlayerRepository
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCase
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCase
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitMatchRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCase
import com.walcker.games.features.ui.create.CreateMatchStep
import com.walcker.games.features.ui.shared.common.LoginRequiredBottomSheet
import com.walcker.games.features.ui.shared.common.icon
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.ConfirmDialog
import com.walcker.games.features.ui.shared.matchDetail.component.IconInfoRow
import com.walcker.games.features.ui.shared.matchDetail.component.JoinBar
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.features.ui.shared.matchDetail.component.LocationAppDialog
import com.walcker.games.features.ui.shared.matchDetail.component.ParticipantsList
import com.walcker.games.features.ui.shared.matchDetail.component.StaticParticipantsList
import com.walcker.games.features.ui.shared.matchDetail.component.StatusBadge
import com.walcker.games.features.ui.shared.ratings.RatingBottomSheet
import com.walcker.games.features.ui.shared.reports.ReportBottomSheet
import com.walcker.games.strings.GamesStrings
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.games.strings.ReportStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.identity.api.SessionHolder
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarSplashLoadingAnimation
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.components.SlotBadge
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.datetime.formatDayLabel
import com.walcker.match.core.datetime.formatTimeRange
import com.walcker.match.navigator.LoginCoordinator
import com.walcker.match.navigator.PromotionCoordinator
import org.koin.compose.koinInject

private val ActionLoadingSize = 28.dp

internal class MatchDetailStep(
    val matchId: String,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        MatchDetailScreenContent(
            matchId = matchId,
            onDismiss = { navigator.pop() },
            onNavigateToConfirmation = { id, venueName, startsAtSeconds, durationMin, sport ->
                navigator.replace(
                    MatchConfirmedStep(
                        matchId = id,
                        venueName = venueName,
                        startsAtSeconds = startsAtSeconds,
                        durationMin = durationMin,
                        sport = sport,
                    ),
                )
            },
            onEditMatch = { id -> navigator.push(CreateMatchStep(id)) },
        )
    }
}

@Composable
internal fun MatchDetailScreenContent(
    matchId: String,
    onDismiss: () -> Unit,
    onNavigateToConfirmation: (
        matchId: String,
        venueName: String,
        startsAtSeconds: Long,
        durationMin: Int,
        sport: Sport,
    ) -> Unit,
    onEditMatch: (matchId: String) -> Unit = {},
) {
    val getGameById: GetGameByIdUseCase = koinInject()
    val observeMatch: ObserveMatchUseCase = koinInject()
    val observeParticipants: ObserveParticipantsUseCase = koinInject()
    val submitRating: SubmitRatingUseCase = koinInject()
    val submitMatchRating: SubmitMatchRatingUseCase = koinInject()
    val submitReport: SubmitReportUseCase = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val sessionHolder: SessionHolder = koinInject()
    val promotionCoordinator: PromotionCoordinator = koinInject()
    val stringsHolder: GamesStringsHolder = koinInject()
    val analytics: AnalyticsTracker = koinInject()
    val crashReporter: CrashReporter = koinInject()
    val loginCoordinator: LoginCoordinator = koinInject()

    val joinGame: JoinGameUseCase = koinInject()
    val leaveGame: LeaveMatchUseCase = koinInject()
    val cancelGame: CancelMatchUseCase = koinInject()
    val cancelSeries: CancelMatchSeriesUseCase = koinInject()

    val stepModel =
        remember {
            MatchDetailStepModel(
                getGameById = getGameById,
                observeMatch = observeMatch,
                observeParticipants = observeParticipants,
                joinGame = joinGame,
                leaveMatch = leaveGame,
                cancelMatch = cancelGame,
                cancelMatchSeries = cancelSeries,
                submitRating = submitRating,
                submitMatchRating = submitMatchRating,
                submitReport = submitReport,
                playerRepository = playerRepository,
                sessionHolder = sessionHolder,
                promotionCoordinator = promotionCoordinator,
                stringsHolder = stringsHolder,
                analytics = analytics,
                crashReporter = crashReporter,
                matchId = matchId,
            )
        }

    val state by stepModel.state.collectAsState()
    val strings = rememberGamesStrings().strings
    val loginRequired = strings.loginRequired
    var showLoginSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stepModel.effects.collect { effect ->
            when (effect) {
                is MatchDetailEffect.NavigateToConfirmation ->
                    onNavigateToConfirmation(
                        effect.matchId,
                        effect.venueName,
                        effect.startsAtSeconds,
                        effect.durationMin,
                        effect.sport,
                    )

                MatchDetailEffect.RequireLogin -> showLoginSheet = true
            }
        }
    }

    MatchDetailContent(
        state = state,
        strings = strings,
        onEvent = stepModel::onEvent,
        onEditMatch = { onEditMatch(matchId) },
        onDismiss = onDismiss,
    )

    LoginRequiredBottomSheet(
        isVisible = showLoginSheet,
        strings = loginRequired,
        onConfirm = {
            loginCoordinator.requestLogin()
            showLoginSheet = false
            onDismiss()
        },
        onDismiss = { showLoginSheet = false },
    )
}

@Composable
internal fun MatchDetailContent(
    state: MatchDetailState,
    strings: GamesStrings,
    onEvent: (MatchDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
    onEditMatch: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val detail = strings.matchDetail

    val match = state.match
    val confirmed = state.participants?.confirmedCount ?: match?.confirmedPlayers ?: 0
    val total = match?.totalPlayers ?: state.participants?.totalSlots ?: 0
    val openSlots = (total - confirmed).coerceAtLeast(0)
    val isFull = match != null && (confirmed >= total || match.status == MatchStatus.FULL)
    val isClosed =
        match != null &&
            (
                state.isMatchOver ||
                    match.status == MatchStatus.FINISHED ||
                    match.status == MatchStatus.CANCELLED
            )
    val isParticipant = state.currentUserId != null && state.currentUserId in (match?.participants ?: emptyList())

    if (state.isJoining && isFull) {
        CedarSplashLoadingAnimation(
            contentDescription = detail.joiningWaitlistLabel,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CedarTokens.colors.canvas),
    ) {
        CedarTopBar(
            title = detail.title,
            onBack = onDismiss,
            backContentDescription = detail.dismissContentDescription,
            leadingIcon = Icons.Default.Close,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            if (state.justPromoted) {
                Banner(
                    message = detail.promotedFromWaitlist,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissPromotion) },
                )
            }
            state.successMessage?.let { message ->
                Banner(
                    message = message,
                    container = CedarTokens.colors.availableContainer,
                    onContainer = CedarTokens.colors.availableText,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissSuccess) },
                )
            }
            state.statusChangeMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissStatusChange) },
                )
            }
            state.ratingErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissRatingError) },
                )
            }
            state.reportErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissReportError) },
                )
            }

            when {
                state.isLoading -> LoadingBlock(contentDescription = detail.loadingLabel)

                state.errorMessage != null ->
                    EmptyState(
                        message = state.errorMessage.orEmpty(),
                        actionLabel = detail.retry,
                        onAction = { onEvent(MatchDetailEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                match != null ->
                    MatchDetailBody(
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
                        isCancellingSeries = state.isCancellingSeries,
                        currentUserId = state.currentUserId,
                        isParticipant = isParticipant,
                        participantRatings = state.participantRatings,
                        reportStrings = strings.reports,
                        onReportPlayer = { userId, displayName ->
                            onEvent(MatchDetailEvent.OpenReportSheet(userId, displayName))
                        },
                        onRatePlayer = { userId, displayName ->
                            onEvent(MatchDetailEvent.OpenRatingSheet(userId, displayName))
                        },
                        onRateMatch = { onEvent(MatchDetailEvent.OpenMatchRatingSheet) },
                        onLeaveMatch = { onEvent(MatchDetailEvent.RequestLeaveMatch) },
                        onCancelMatch = { onEvent(MatchDetailEvent.RequestCancelMatch) },
                        onCancelMatchSeries = { onEvent(MatchDetailEvent.RequestCancelSeries) },
                        onEditMatch = onEditMatch,
                    )

                else ->
                    EmptyState(
                        message = detail.notFound,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
        }

        if (match != null && !isParticipant) {
            JoinBar(
                label =
                    when {
                        isClosed -> detail.matchClosed
                        isFull -> detail.joinWaitlist
                        else -> detail.joinMatch
                    },
                priceLabel = match.pricePerPlayer,
                enabled = !isClosed && !state.isJoining,
                isLoading = state.isJoining && !isFull,
                useAvailabilityTone = !isClosed && !isFull,
                onClick = { onEvent(MatchDetailEvent.JoinMatch) },
            )
        }
    }

    ReportBottomSheet(
        isVisible = state.showReportSheet,
        playerName = state.selectedPlayerForReport?.second ?: "",
        strings = strings.reports,
        isSubmitting = state.isSubmittingReport,
        onDismiss = { onEvent(MatchDetailEvent.CloseReportSheet) },
        onSubmit = { reason, details ->
            onEvent(MatchDetailEvent.SubmitReport(reason, details))
        },
    )

    RatingBottomSheet(
        isVisible = state.showRatingSheet,
        playerName = state.selectedPlayerForRating?.second ?: "",
        strings = strings.ratings,
        onDismiss = { onEvent(MatchDetailEvent.CloseRatingSheet) },
        onSubmit = { rating, comment, dimensions ->
            onEvent(MatchDetailEvent.SubmitRating(rating, comment, dimensions))
        },
        isLoading = state.isSubmittingRating,
    )

    MatchRatingBottomSheet(
        isVisible = state.showMatchRatingSheet,
        strings = detail,
        starContentDescription = strings.ratings.starContentDescription,
        onDismiss = { onEvent(MatchDetailEvent.CloseMatchRatingSheet) },
        onSubmit = { rating -> onEvent(MatchDetailEvent.SubmitMatchRating(rating)) },
        isLoading = state.isSubmittingMatchRating,
    )

    if (state.showLeaveConfirmDialog) {
        ConfirmDialog(
            title = detail.leaveDialogTitle,
            body = detail.leaveDialogBody,
            confirmLabel = detail.leaveDialogConfirm,
            dismissLabel = detail.dialogDismiss,
            isWorking = state.isLeavingMatch,
            onConfirm = { onEvent(MatchDetailEvent.ConfirmLeaveMatch) },
            onDismiss = { onEvent(MatchDetailEvent.CancelLeaveMatch) },
        )
    }

    if (state.showCancelConfirmDialog) {
        ConfirmDialog(
            title = detail.cancelDialogTitle,
            body = detail.cancelDialogBody,
            confirmLabel = detail.cancelDialogConfirm,
            dismissLabel = detail.dialogDismiss,
            isWorking = state.isCancellingMatch,
            onConfirm = { onEvent(MatchDetailEvent.ConfirmCancelMatch) },
            onDismiss = { onEvent(MatchDetailEvent.CancelCancelMatch) },
        )
    }

    if (state.showCancelSeriesConfirmDialog) {
        ConfirmDialog(
            title = detail.cancelSeriesDialogTitle,
            body = detail.cancelSeriesDialogBody,
            confirmLabel = detail.cancelSeriesDialogConfirm,
            dismissLabel = detail.dialogDismiss,
            isWorking = state.isCancellingSeries,
            onConfirm = { onEvent(MatchDetailEvent.ConfirmCancelSeries) },
            onDismiss = { onEvent(MatchDetailEvent.CancelCancelSeries) },
        )
    }
}

@Composable
internal fun MatchDetailBody(
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
    isParticipant: Boolean,
    participantRatings: Map<String, PlayerRatingSummary>,
    reportStrings: ReportStrings,
    onReportPlayer: (userId: String, displayName: String) -> Unit,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    onRateMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onCancelMatch: () -> Unit,
    onCancelMatchSeries: () -> Unit,
    onEditMatch: () -> Unit,
    isLeavingMatch: Boolean,
    isCancellingMatch: Boolean,
    isCancellingSeries: Boolean,
    modifier: Modifier = Modifier,
) {
    var showLocationChooser by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Text(
                text = match.venueName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SlotBadge(label = detail.slotsBadge(openSlots), openSlots = openSlots)
        }

        if (match.matchRatingCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                RatingStars(rating = match.matchRating.toFloat())
                Text(
                    text = detail.ratingsCount(match.matchRatingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isClosed) {
            StatusBadge(status = match.status, isMatchOver = isMatchOver, detail = detail)
        }

        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
            IconInfoRow(
                icon = Icons.Filled.CalendarMonth,
                text = formatDayLabel(startsAtSeconds = match.startsAtSeconds),
            )
            IconInfoRow(
                icon = Icons.Filled.Schedule,
                text = "${formatTimeRange(match.startsAtSeconds, match.durationMin)} (${detail.durationValue(match.durationMin)})",
            )
            IconInfoRow(
                icon = Icons.Filled.LocationOn,
                text = "${match.neighborhood} · ${match.city}",
                onClick = { showLocationChooser = true },
            )
            IconInfoRow(
                icon = match.sport.icon(),
                text = match.sport.label,
            )
            IconInfoRow(
                icon = Icons.Filled.Payments,
                text = match.pricePerPlayer ?: detail.freePrice,
            )
            IconInfoRow(
                icon = Icons.Filled.Groups,
                text = detail.confirmedOf(confirmed, total),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = detail.organizedBy(match.organizerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (match.organizerRatingCount > 0) {
                    RatingStars(rating = match.organizerRating.toFloat(), starSize = 12.dp)
                    Text(
                        text = detail.ratingsCount(match.organizerRatingCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (canRate) {
            CedarSecondaryButton(
                text = detail.rateMatchAction,
                onClick = onRateMatch,
                fillWidth = false,
            )
        }

        CedarSectionHeader(title = detail.participants)

        if (participants != null) {
            ParticipantsList(
                participants = participants,
                detail = detail,
                canRate = canRate,
                currentUserId = currentUserId,
                participantRatings = participantRatings,
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

        val isOrganizer = currentUserId != null && currentUserId == match.organizerId

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            if (isOrganizer) {
                CedarSecondaryButton(
                    text = detail.editMatch,
                    onClick = onEditMatch,
                    enabled = !isClosed,
                    fillWidth = false,
                    modifier = Modifier.weight(1f),
                )
                if (isCancellingMatch) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CedarLoading(contentDescription = detail.cancelMatch, size = ActionLoadingSize)
                    }
                } else {
                    CedarSecondaryButton(
                        text = detail.cancelMatch,
                        onClick = onCancelMatch,
                        enabled = !isClosed,
                        fillWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (isParticipant) {
                if (isLeavingMatch) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CedarLoading(contentDescription = detail.leaveMatch, size = ActionLoadingSize)
                    }
                } else {
                    CedarSecondaryButton(
                        text = detail.leaveMatch,
                        onClick = onLeaveMatch,
                        enabled = !isClosed,
                        fillWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (isOrganizer && match.recurrence != RecurrenceOption.NONE) {
            if (isCancellingSeries) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CedarLoading(contentDescription = detail.cancelMatchSeries, size = ActionLoadingSize)
                }
            } else {
                CedarTextButton(
                    text = detail.cancelMatchSeries,
                    onClick = onCancelMatchSeries,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showLocationChooser) {
        LocationAppDialog(
            title = detail.openLocationTitle,
            googleMapsLabel = detail.openInGoogleMaps,
            wazeLabel = detail.openInWaze,
            cancelLabel = detail.openLocationCancel,
            onGoogleMaps = {
                uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=${match.lat},${match.lng}")
                showLocationChooser = false
            },
            onWaze = {
                uriHandler.openUri("https://waze.com/ul?ll=${match.lat},${match.lng}&navigate=yes")
                showLocationChooser = false
            },
            onDismiss = { showLocationChooser = false },
        )
    }
}
