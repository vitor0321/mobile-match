package com.walcker.games.features.ui.shared.playerDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.ui.shared.playerDetails.component.DimensionAveragesCard
import com.walcker.games.features.ui.shared.playerDetails.component.LoadingContent
import com.walcker.games.features.ui.shared.playerDetails.component.PlayerHeader
import com.walcker.games.features.ui.shared.playerDetails.component.RatingCard
import com.walcker.games.features.ui.shared.playerRatings.PlayerRatingsListStep
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.games.strings.RatingStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.RatingSummary
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.parameter.parametersOf

private val ReviewsLoadingHeight = 100.dp

internal data class PlayerDetailsStep(
    val userId: String,
) : Screen {
    override val key: String get() = "player-details-$userId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<PlayerDetailsStepModel>(
                parameters = { parametersOf(userId) },
            )
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val allStrings = rememberGamesStrings().strings
        val strings = allStrings.playerDetails
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
            val contentModifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)

            val player = state.player
            when {
                state.isLoadingPlayer ->
                    LoadingContent(
                        contentDescription = strings.loadingLabel,
                        modifier = contentModifier,
                    )

                player == null ->
                    EmptyState(
                        message = state.errorMessage ?: strings.errorLoading,
                        actionLabel = strings.retry,
                        onAction = { stepModel.onEvent(PlayerDetailsEvents.RetryLoading) },
                        modifier = contentModifier,
                    )

                else ->
                    PlayerDetailsContent(
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
        contentPadding =
            PaddingValues(
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
            state.isLoadingRatings ->
                item(key = "reviews-loading") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ReviewsLoadingHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

            state.previewRatings.isEmpty() ->
                item(key = "reviews-empty") {
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
                        ratingAccessibilityLabel =
                            strings.ratingAccessibility(
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
