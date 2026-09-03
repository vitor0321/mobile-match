package com.walcker.games.features.ui.playerProfile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.ui.playerProfile.component.AvailabilityCard
import com.walcker.games.features.ui.playerProfile.component.ProfileHeader
import com.walcker.games.features.ui.playerProfile.component.RatingItemCard
import com.walcker.games.features.ui.shared.common.LoginRequiredBottomSheet
import com.walcker.games.features.ui.shared.playerDetails.PlayerDetailsStep
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStat
import com.walcker.match.cedar.components.CedarStatRow
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.format.formatDecimal
import com.walcker.match.navigator.LoginCoordinator
import com.walcker.match.navigator.MatchDetailCoordinator
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject

internal class PlayerProfileStep : Screen {
    @Composable
    override fun Content() {
        val strings = rememberGamesStrings().strings.playerProfile
        val model = koinScreenModel<PlayerProfileStepModel>()
        val state by model.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val loginCoordinator: LoginCoordinator = koinInject()
        val matchDetailCoordinator: MatchDetailCoordinator = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val loginRequired = rememberGamesStrings().strings.loginRequired
        var showLoginSheet by remember { mutableStateOf(false) }

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
                    is PlayerProfileEffect.NavigateToOwnPublicProfile ->
                        navigator.push(PlayerDetailsStep(effect.userId))
                }
            }
        }

        PlayerProfileContent(
            state = state,
            onEvent = model::onEvent,
            strings = strings,
            onLoginRequested = { loginCoordinator.requestLogin() },
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
    }
}

@Composable
internal fun PlayerProfileContent(
    state: PlayerProfileState,
    onEvent: (PlayerProfileEvent) -> Unit,
    strings: PlayerProfileStrings,
    modifier: Modifier = Modifier,
    onLoginRequested: () -> Unit = {},
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
                        ),
            ) {
                CedarScreenTitle(title = strings.title)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        message = strings.visitorMessage,
                        supportingText = strings.visitorSupportingText,
                        actionLabel = strings.visitorCta,
                        onAction = onLoginRequested,
                    )
                }
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
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
        ) {
            item {
                CedarScreenTitle(title = strings.title)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
                    ProfileHeader(
                        name = state.userName,
                        email = state.userEmail,
                        fallbackName = strings.fallbackAccountName,
                    )
                    CedarTextButton(
                        text = strings.viewPublicProfileAction,
                        onClick = { onEvent(PlayerProfileEvent.ViewPublicProfileClicked) },
                    )
                }
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
                    availableSports = state.availableSports,
                    onSportToggled = { sport -> onEvent(PlayerProfileEvent.SportToggled(sport)) },
                )
            }

            item {
                CedarStatRow(
                    stats =
                        persistentListOf(
                            CedarStat(
                                value = state.matchesOrganized.toString(),
                                label = strings.statsOrganized,
                            ),
                            CedarStat(
                                value = state.matchesParticipated.toString(),
                                label = strings.statsParticipated,
                            ),
                            CedarStat(
                                value =
                                    if (state.totalRatings > 0) {
                                        formatDecimal(value = state.averageRating, decimals = 1)
                                    } else {
                                        strings.noRatingYet
                                    },
                                label = strings.statsRating,
                                highlighted = state.totalRatings > 0,
                            ),
                        ),
                )
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
                    Text(
                        text = strings.settings,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CedarSecondaryButton(
                        text = strings.logout,
                        onClick = { onEvent(PlayerProfileEvent.LogoutRequested) },
                    )
                }
            }
        }
    }
}
