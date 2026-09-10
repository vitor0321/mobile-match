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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.PlayerRepository
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCase
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitMatchRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitOrganizerRatingUseCase
import com.walcker.games.features.ui.shared.common.LoginRequiredBottomSheet
import com.walcker.games.features.ui.shared.common.icon
import com.walcker.games.features.ui.shared.manageMatch.ManageMatchStep
import com.walcker.games.features.ui.shared.matchDetail.component.Banner
import com.walcker.games.features.ui.shared.matchDetail.component.ConfirmDialog
import com.walcker.games.features.ui.shared.matchDetail.component.IconInfoRow
import com.walcker.games.features.ui.shared.matchDetail.component.JoinBar
import com.walcker.games.features.ui.shared.matchDetail.component.LoadingBlock
import com.walcker.games.features.ui.shared.matchDetail.component.LocationAppDialog
import com.walcker.games.features.ui.shared.matchDetail.component.StatusBadge
import com.walcker.games.features.ui.shared.matchDetail.component.YourParticipationStatus
import com.walcker.games.features.ui.shared.matchDetail.component.YourTeamAssignment
import com.walcker.games.strings.GamesStrings
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.identity.api.SessionHolder
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarSplashLoadingAnimation
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
            onManageMatch = { id -> navigator.push(ManageMatchStep(id)) },
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
    onManageMatch: (matchId: String) -> Unit = {},
) {
    val getGameById: GetGameByIdUseCase = koinInject()
    val observeMatch: ObserveMatchUseCase = koinInject()
    val observeParticipants: ObserveParticipantsUseCase = koinInject()
    val submitMatchRating: SubmitMatchRatingUseCase = koinInject()
    val submitOrganizerRating: SubmitOrganizerRatingUseCase = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val sessionHolder: SessionHolder = koinInject()
    val promotionCoordinator: PromotionCoordinator = koinInject()
    val stringsHolder: GamesStringsHolder = koinInject()
    val analytics: AnalyticsTracker = koinInject()
    val crashReporter: CrashReporter = koinInject()
    val loginCoordinator: LoginCoordinator = koinInject()

    val joinGame: JoinGameUseCase = koinInject()
    val leaveGame: LeaveMatchUseCase = koinInject()

    val stepModel =
        remember {
            MatchDetailStepModel(
                getGameById = getGameById,
                observeMatch = observeMatch,
                observeParticipants = observeParticipants,
                joinGame = joinGame,
                leaveMatch = leaveGame,
                submitMatchRating = submitMatchRating,
                submitOrganizerRating = submitOrganizerRating,
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
        onManageMatch = { onManageMatch(matchId) },
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
    onManageMatch: () -> Unit = {},
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
    val isOrganizer = match != null && state.currentUserId != null && state.currentUserId == match.organizerId

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
        val shareLauncher = rememberShareLauncher()

        CedarTopBar(
            title = detail.title,
            onBack = onDismiss,
            backContentDescription = detail.dismissContentDescription,
            leadingIcon = Icons.Default.Close,
            actions = {
                if (match != null) {
                    IconButton(
                        onClick = {
                            shareLauncher(
                                detail.shareSubject(match.sport.label),
                                detail.shareMessage(
                                    match.sport.label,
                                    formatDayLabel(match.startsAtSeconds),
                                    formatTimeRange(match.startsAtSeconds, match.durationMin),
                                    match.venueName,
                                    match.address,
                                    "https://vitor0321.github.io/match/?id=${match.id}",
                                ),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = detail.shareContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
            state.actionErrorMessage?.let { message ->
                Banner(
                    message = message,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    dismissContentDescription = detail.dismissContentDescription,
                    onDismiss = { onEvent(MatchDetailEvent.DismissActionError) },
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
                        canRateOrganizer = state.canRateOrganizer,
                        organizerRatingSummary = state.organizerRatingSummary,
                        onRateOrganizer = { onEvent(MatchDetailEvent.OpenOrganizerRatingSheet) },
                        isMatchOver = state.isMatchOver,
                        isLeavingMatch = state.isLeavingMatch,
                        currentUserId = state.currentUserId,
                        isParticipant = isParticipant,
                        isJoining = state.isJoining,
                        onRateMatch = { onEvent(MatchDetailEvent.OpenMatchRatingSheet) },
                        onJoinMatch = { onEvent(MatchDetailEvent.JoinMatch) },
                        onLeaveMatch = { onEvent(MatchDetailEvent.RequestLeaveMatch) },
                        onManageMatch = onManageMatch,
                    )

                else ->
                    EmptyState(
                        message = detail.notFound,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
        }

        if (match != null && !isParticipant && !isOrganizer) {
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

    MatchRatingBottomSheet(
        isVisible = state.showMatchRatingSheet,
        strings = detail,
        starContentDescription = strings.ratings.starContentDescription,
        onDismiss = { onEvent(MatchDetailEvent.CloseMatchRatingSheet) },
        onSubmit = { rating -> onEvent(MatchDetailEvent.SubmitMatchRating(rating)) },
        isLoading = state.isSubmittingMatchRating,
    )

    OrganizerRatingBottomSheet(
        isVisible = state.showOrganizerRatingSheet,
        strings = detail,
        starContentDescription = strings.ratings.starContentDescription,
        onDismiss = { onEvent(MatchDetailEvent.CloseOrganizerRatingSheet) },
        onSubmit = { rating -> onEvent(MatchDetailEvent.SubmitOrganizerRating(rating)) },
        isLoading = state.isSubmittingOrganizerRating,
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
    canRateOrganizer: Boolean,
    organizerRatingSummary: PlayerRatingSummary?,
    onRateOrganizer: () -> Unit,
    isMatchOver: Boolean,
    currentUserId: String?,
    isParticipant: Boolean,
    onRateMatch: () -> Unit,
    onJoinMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onManageMatch: () -> Unit,
    isJoining: Boolean,
    isLeavingMatch: Boolean,
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
            RatingStars(rating = match.matchRating.toFloat())
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.organizedBy(match.organizerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (organizerRatingSummary != null && organizerRatingSummary.ratingCount > 0) {
                        RatingStars(rating = organizerRatingSummary.rating, starSize = 12.dp)
                    }
                }
                if (canRateOrganizer) {
                    IconButton(onClick = onRateOrganizer) {
                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = detail.rateOrganizerAction,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
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

        if (isParticipant && currentUserId != null) {
            val yourEntry =
                participants?.confirmed?.find { it.userId == currentUserId }
                    ?: participants?.waitlist?.find { it.userId == currentUserId }
            YourParticipationStatus(
                isConfirmed = yourEntry?.isConfirmed ?: true,
                waitlistPosition = yourEntry?.positionInWaitlist,
                detail = detail,
            )
            val yourTeamIndex = match.teamAssignments[currentUserId]
            if (match.teamCount > 0 && yourTeamIndex != null) {
                YourTeamAssignment(teamIndex = yourTeamIndex, detail = detail)
            }
        }

        CedarSectionHeader(title = detail.participants)

        Text(
            text = detail.confirmedSection(confirmed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detail.waitlistSection(participants?.waitlistCount ?: 0),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val isOrganizer = currentUserId != null && currentUserId == match.organizerId

        if (isOrganizer) {
            CedarSecondaryButton(
                text = detail.manageMatchAction,
                onClick = onManageMatch,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (isParticipant) {
            if (isLeavingMatch) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CedarLoading(contentDescription = detail.leaveMatch, size = ActionLoadingSize)
                }
            } else {
                CedarSecondaryButton(
                    text = detail.leaveMatch,
                    onClick = onLeaveMatch,
                    enabled = !isClosed,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isOrganizer) {
            if (isParticipant) {
                if (isLeavingMatch) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CedarLoading(contentDescription = detail.leaveMatch, size = ActionLoadingSize)
                    }
                } else {
                    CedarSecondaryButton(
                        text = detail.leaveMatch,
                        onClick = onLeaveMatch,
                        enabled = !isClosed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                if (isJoining) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CedarLoading(contentDescription = detail.participateAction, size = ActionLoadingSize)
                    }
                } else {
                    CedarPrimaryButton(
                        text = detail.participateAction,
                        onClick = onJoinMatch,
                        enabled = !isClosed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
