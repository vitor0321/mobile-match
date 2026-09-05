package com.walcker.games.features.ui.playerProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.about.AboutStep
import com.walcker.games.features.ui.playerProfile.component.AvailabilityCard
import com.walcker.games.features.ui.playerProfile.component.MySportsSection
import com.walcker.games.features.ui.playerProfile.component.ProfileHeader
import com.walcker.games.features.ui.playerProfile.component.RatingItemCard
import com.walcker.games.features.ui.shared.common.LoginRequiredBottomSheet
import com.walcker.games.features.ui.shared.notifications.NotificationHistoryStep
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarMenuRow
import com.walcker.match.cedar.components.CedarProfilePreLoginAnimation
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStat
import com.walcker.match.cedar.components.CedarStatRow
import com.walcker.match.cedar.components.LocalBottomBarInset
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.format.formatDecimal
import com.walcker.match.navigator.LoginCoordinator
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.MatchDetailCoordinator
import com.walcker.match.navigator.TabCoordinator
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject

private val PreLoginAnimationSize = 280.dp
private val FeatureBadgeSize = 32.dp
private val FeatureIconSize = 18.dp

internal class PlayerProfileStep : Screen {
    @Composable
    override fun Content() {
        val strings = rememberGamesStrings().strings.playerProfile
        val model = koinScreenModel<PlayerProfileStepModel>()
        val state by model.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val loginCoordinator: LoginCoordinator = koinInject()
        val matchDetailCoordinator: MatchDetailCoordinator = koinInject()
        val tabCoordinator: TabCoordinator = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val loginRequired = rememberGamesStrings().strings.loginRequired
        var showLoginSheet by remember { mutableStateOf(false) }
        var showNotifications by remember { mutableStateOf(false) }

        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissError)
            }
        }

        LaunchedEffect(state.availabilityErrorMessage) {
            state.availabilityErrorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissAvailabilityError)
            }
        }

        LaunchedEffect(state.sportsErrorMessage) {
            state.sportsErrorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissSportsError)
            }
        }

        LaunchedEffect(model) {
            model.effects.collect { effect ->
                when (effect) {
                    PlayerProfileEffect.RequireLogin -> showLoginSheet = true
                    is PlayerProfileEffect.NavigateToMatchDetail ->
                        matchDetailCoordinator.open(effect.matchId)
                }
            }
        }

        PlayerProfileContent(
            state = state,
            onEvent = model::onEvent,
            strings = strings,
            onLoginRequested = { loginCoordinator.requestLogin() },
            onNotificationsClicked = { showNotifications = true },
            onMyMatchesClicked = { tabCoordinator.requestTab(MainTab.MyMatches) },
            onAboutClicked = { navigator.push(AboutStep()) },
            snackbarHostState = snackbarHostState,
        )

        LoginRequiredBottomSheet(
            isVisible = showLoginSheet,
            strings = loginRequired,
            onConfirm = {
                loginCoordinator.requestLogin()
                showLoginSheet = false
            },
            onDismiss = { showLoginSheet = false },
        )

        NotificationHistoryStep(
            isVisible = showNotifications,
            onDismiss = { showNotifications = false },
        )
    }
}

@Composable
internal fun PlayerProfileContent(
    state: PlayerProfileState,
    onEvent: (PlayerProfileEvent) -> Unit,
    strings: PlayerProfileStrings,
    modifier: Modifier = Modifier,
    onLoginRequested: () -> Unit = {},
    onNotificationsClicked: () -> Unit = {},
    onMyMatchesClicked: () -> Unit = {},
    onAboutClicked: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CedarLoading(contentDescription = strings.loadingLabel)
            }
            return@Scaffold
        }

        if (state.userId == null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.md,
                        )
                        .padding(bottom = LocalBottomBarInset.current),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                CedarScreenTitle(title = strings.title)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
                    ) {
                        CedarProfilePreLoginAnimation(
                            contentDescription = null,
                            modifier = Modifier.size(PreLoginAnimationSize),
                        )
                        Text(
                            text = strings.visitorHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                        ) {
                            VisitorFeatureItem(
                                icon = Icons.Default.SportsSoccer,
                                text = strings.visitorFeatureSports,
                            )
                            VisitorFeatureItem(
                                icon = Icons.Default.Groups,
                                text = strings.visitorFeatureConnections,
                            )
                            VisitorFeatureItem(
                                icon = Icons.Default.Person,
                                text = strings.visitorFeatureYou,
                            )
                        }
                    }
                }
                CedarSecondaryButton(
                    text = strings.visitorCta,
                    onClick = onLoginRequested,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            contentPadding =
                PaddingValues(
                    start = CedarTokens.spacing.lg,
                    end = CedarTokens.spacing.lg,
                    top = CedarTokens.spacing.md,
                    bottom = CedarTokens.spacing.md + LocalBottomBarInset.current,
                ),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNotificationsClicked) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = strings.notificationsContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            item {
                ProfileHeader(
                    name = state.userName,
                    email = state.userEmail,
                    fallbackName = strings.fallbackAccountName,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
                    CedarStatRow(
                        stats =
                            persistentListOf(
                                CedarStat(
                                    value = (state.matchesOrganized + state.matchesParticipated).toString(),
                                    label = strings.statsMatches,
                                ),
                                CedarStat(
                                    value = state.matchesParticipated.toString(),
                                    label = strings.statsParticipated,
                                ),
                                CedarStat(
                                    value = state.totalRatings.toString(),
                                    label = strings.statsReviews,
                                ),
                            ),
                    )
                    if (state.totalRatings > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RatingStars(
                                rating = state.averageRating,
                                contentDescription = strings.ratingContentDescription(state.averageRating),
                            )
                            Text(
                                text = "${formatDecimal(state.averageRating, 1)} (${strings.ratingsCount(state.totalRatings)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                AvailabilityCard(
                    isAvailable = state.isAvailable,
                    isUpdating = state.isUpdatingAvailability,
                    strings = strings,
                    onCheckedChange = { checked ->
                        onEvent(PlayerProfileEvent.AvailabilityChanged(checked))
                    },
                    availableUntilTonight = state.availableUntilMs != null,
                    onUntilTonightChange = { enabled ->
                        onEvent(PlayerProfileEvent.AvailableUntilTonightToggled(enabled))
                    },
                )
            }

            item {
                MySportsSection(
                    availableSports = state.availableSports,
                    strings = strings,
                    onSportToggled = { sport -> onEvent(PlayerProfileEvent.SportToggled(sport)) },
                )
            }

            val nextMatch = state.nextMatch
            if (nextMatch != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                        CedarSectionHeader(title = strings.nextMatchSection)
                        MatchCard(
                            venueName = nextMatch.game.venueName,
                            startsAtSeconds = nextMatch.game.startsAtSeconds,
                            onClick = { onEvent(PlayerProfileEvent.NextMatchClicked(nextMatch.game.id)) },
                            metaLabel = "${nextMatch.game.sport.label} · ${nextMatch.game.neighborhood}",
                        )
                    }
                }
            }

            if (state.totalRatings > 0) {
                item {
                    CedarSectionHeader(
                        title = strings.ratingsReceived,
                        subtitle = strings.ratingsCount(state.totalRatings),
                    )
                }
                items(items = state.ratings, key = { it.id }) { rating ->
                    RatingItemCard(rating = rating, strings = strings)
                }
            }

            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = CedarTokens.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                ) {
                    CedarMenuRow(
                        icon = Icons.Default.SportsSoccer,
                        label = strings.myMatchesMenuLabel,
                        onClick = onMyMatchesClicked,
                    )
                    CedarMenuRow(
                        icon = Icons.Outlined.Info,
                        label = strings.aboutMenuLabel,
                        onClick = onAboutClicked,
                    )
                }
            }

            item {
                CedarSecondaryButton(
                    text = strings.logout,
                    onClick = { onEvent(PlayerProfileEvent.LogoutRequested) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = CedarTokens.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun VisitorFeatureItem(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(FeatureBadgeSize)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(FeatureIconSize),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
