package com.walcker.games.features.ui.player_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.ui.player_ratings.PlayerRatingsListStep
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.games.strings.RatingStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.components.RatingSummary
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.parameter.parametersOf

private val ReviewsLoadingHeight = 100.dp
private val HeaderStarSize = 20.dp

/**
 * Another player's profile: identity, rating distribution and received reviews.
 *
 * Deliberately no experience stats. Matches organized/played and join/cancel rates
 * were removed in Sprint 3 — no writer produces them, so the section only ever
 * rendered zeros. They return in Phase 6 with the trigger that keeps them.
 */
internal data class PlayerDetailsStep(val userId: String) : Screen {

    override val key: String get() = "player-details-$userId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<PlayerDetailsStepModel>(
            parameters = { parametersOf(userId) },
        )
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val allStrings = rememberGamesStrings().strings
        val strings = allStrings.playerDetails
        // Os rótulos das quatro dimensões vivem em RatingStrings, junto com o
        // formulário que as coleta.
        val ratingStrings = allStrings.ratings

        LaunchedEffect(stepModel) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is PlayerDetailsEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)

                    is PlayerDetailsEffect.NavigateToRatings ->
                        navigator.push(
                            PlayerRatingsListStep(
                                userId = effect.userId,
                                playerName = effect.playerName,
                            ),
                        )
                }
            }
        }

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            topBar = {
                CedarTopBar(
                    title = strings.title,
                    onBack = navigator::pop,
                    backContentDescription = strings.back,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(padding)

            val player = state.player
            when {
                state.isLoadingPlayer -> LoadingContent(modifier = contentModifier)

                player == null -> EmptyState(
                    message = state.errorMessage ?: strings.errorLoading,
                    actionLabel = strings.retry,
                    onAction = { stepModel.onEvent(PlayerDetailsEvents.RetryLoading) },
                    modifier = contentModifier,
                )

                else -> PlayerDetailsContent(
                    state = state,
                    player = player,
                    strings = strings,
                    ratingStrings = ratingStrings,
                    onEvent = stepModel::onEvent,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlayerDetailsContent(
    state: PlayerDetailsState,
    player: PlayerDetails,
    strings: PlayerDetailsStrings,
    ratingStrings: RatingStrings,
    onEvent: (PlayerDetailsEvents) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = CedarTokens.spacing.lg,
            vertical = CedarTokens.spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        item(key = "header") {
            PlayerHeader(player = player, strings = strings)
        }

        if (state.distribution.total > 0) {
            item(key = "distribution") {
                val distribution = state.distribution
                val counts = remember(distribution) { distribution.counts.toImmutableList() }
                RatingSummary(
                    average = distribution.average,
                    averageLabel = strings.ratingValue(distribution.average),
                    totalLabel = strings.ratingsCount(distribution.total),
                    distribution = counts,
                )
            }
        }

        // Some inteira quando ninguém respondeu dimensão nenhuma, que é o estado
        // de todo perfil avaliado antes das dimensões existirem.
        if (player.dimensionAverages.isNotEmpty()) {
            item(key = "dimensions") {
                DimensionAveragesCard(
                    averages = player.dimensionAverages,
                    strings = strings,
                    ratingStrings = ratingStrings,
                )
            }
        }

        item(key = "reviews-title") {
            CedarSectionHeader(title = strings.reviews)
        }

        when {
            state.isLoadingRatings -> item(key = "reviews-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ReviewsLoadingHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.previewRatings.isEmpty() -> item(key = "reviews-empty") {
                Text(
                    text = strings.noReviews,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                items(state.previewRatings, key = { it.id }) { rating ->
                    RatingCard(
                        rating = rating,
                        ratingLabel = strings.ratingValue(rating.rating.toFloat()),
                        ratingAccessibilityLabel = strings.ratingAccessibility(
                            rating.rating.toFloat(),
                        ),
                    )
                }

                if (state.hasMoreRatings) {
                    item(key = "reviews-see-all") {
                        TextButton(
                            onClick = { onEvent(PlayerDetailsEvents.SeeAllRatingsClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = strings.seeAllReviews,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    player: PlayerDetails,
    strings: PlayerDetailsStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        // Was a bare grey circle whenever photoUrl was null — which is most
        // profiles. PlayerAvatar falls back to initials.
        PlayerAvatar(
            displayName = player.displayName,
            photoUrl = player.photoUrl,
            size = PlayerAvatarSize.Large,
        )

        Text(
            text = player.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        RatingStars(
            rating = player.averageRating,
            starSize = HeaderStarSize,
            contentDescription = strings.ratingAccessibility(player.averageRating),
        )
        Text(
            text = strings.ratingValue(player.averageRating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = strings.ratingsCount(player.totalRatings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        player.favoriteSports
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " · ") { it.label }
            ?.let { sports ->
                Text(
                    text = sports,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

        player.locationLabel()?.let { location ->
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val memberSince = formatShortDate(player.memberSinceMs)
        if (memberSince.isNotEmpty()) {
            Text(
                text = strings.memberSince(memberSince),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Bairro · Cidade", skipping whichever half the profile left blank. */
private fun PlayerDetails.locationLabel(): String? = listOfNotNull(
    neighborhood?.takeIf { it.isNotBlank() },
    city?.takeIf { it.isNotBlank() },
).takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")
