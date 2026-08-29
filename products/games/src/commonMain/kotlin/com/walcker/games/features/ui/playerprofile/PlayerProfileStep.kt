package com.walcker.games.features.ui.playerprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.ui.common.LoginRequiredBottomSheet
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStat
import com.walcker.match.cedar.components.CedarStatRow
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate
import com.walcker.match.core.format.formatDecimal
import com.walcker.match.navigator.LoginCoordinator
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

        LaunchedEffect(model) {
            model.effects.collect { effect ->
                when (effect) {
                    PlayerProfileEffect.RequireLogin -> showLoginSheet = true
                }
            }
        }

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CedarLoading(contentDescription = strings.loadingLabel)
                }
                return@Scaffold
            }

            if (state.userName == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CedarScreenTitle(title = strings.title)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            message = strings.visitorMessage,
                            supportingText = strings.visitorSupportingText,
                            actionLabel = strings.visitorCta,
                            onAction = { loginCoordinator.requestLogin() },
                        )
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
                contentPadding = PaddingValues(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
            ) {
                item {
                    CedarScreenTitle(title = strings.title)
                }

                item {
                    ProfileHeader(
                        name = state.userName,
                        email = state.userEmail,
                    )
                }

                item {
                    AvailabilityCard(
                        isAvailable = state.isAvailable,
                        isUpdating = state.isUpdatingAvailability,
                        strings = strings,
                        onCheckedChange = { checked ->
                            model.onEvent(PlayerProfileEvent.AvailabilityChanged(checked))
                        },
                    )
                }

                item {
                    CedarStatRow(
                        stats = persistentListOf(
                            CedarStat(
                                value = state.matchesOrganized.toString(),
                                label = strings.statsOrganized,
                            ),
                            CedarStat(
                                value = state.matchesParticipated.toString(),
                                label = strings.statsParticipated,
                            ),
                            CedarStat(
                                value = if (state.totalRatings > 0) {
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
                        modifier = Modifier
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
                            onClick = { model.onEvent(PlayerProfileEvent.LogoutRequested) },
                        )
                    }
                }
            }
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
private fun ProfileHeader(
    name: String?,
    email: String?,
    modifier: Modifier = Modifier,
) {
    if (name == null) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(displayName = name, size = PlayerAvatarSize.Large)
        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (email != null) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AvailabilityCard(
    isAvailable: Boolean,
    isUpdating: Boolean,
    strings: PlayerProfileStrings,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.availabilityTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isAvailable) {
                        strings.availabilityOnDescription
                    } else {
                        strings.availabilityOffDescription
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isAvailable,
                onCheckedChange = onCheckedChange,
                enabled = !isUpdating,
            )
        }
    }
}

@Composable
private fun RatingItemCard(
    rating: Rating,
    strings: PlayerProfileStrings,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(CedarTokens.spacing.md)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(
                    rating = rating.rating.toFloat(),
                    contentDescription = strings.ratingContentDescription(rating.rating.toFloat()),
                )
                Text(
                    text = formatShortDate(epochMillis = rating.createdAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rating.comment.isNotEmpty()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
