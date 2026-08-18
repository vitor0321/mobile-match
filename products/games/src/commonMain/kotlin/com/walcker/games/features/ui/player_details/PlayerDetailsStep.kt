package com.walcker.games.features.ui.player_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.ui.player_ratings.PlayerRatingsListStep
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.components.RatingSummary
import com.walcker.match.core.datetime.formatShortDate
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.parameter.parametersOf

/**
 * Player details screen — profile, experience stats and received reviews.
 *
 * Stateless composables below the screen boundary: the step model owns the
 * state, this file only maps it to UI and forwards events.
 */
internal data class PlayerDetailsStep(val userId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<PlayerDetailsStepModel>(
            parameters = { parametersOf(userId) },
        )
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.playerDetails

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
            topBar = {
                TopAppBar(
                    title = { Text(strings.title) },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                            )
                        }
                    },
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

                player == null -> ErrorContent(
                    message = state.errorMessage ?: strings.errorLoading,
                    retryLabel = strings.retry,
                    onRetry = { stepModel.onEvent(PlayerDetailsEvents.RetryLoading) },
                    modifier = contentModifier,
                )

                else -> PlayerDetailsContent(
                    state = state,
                    player = player,
                    strings = strings,
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
private fun ErrorContent(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(retryLabel)
        }
    }
}

@Composable
private fun PlayerDetailsContent(
    state: PlayerDetailsState,
    player: PlayerDetails,
    strings: PlayerDetailsStrings,
    onEvent: (PlayerDetailsEvents) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            PlayerHeader(player = player, strings = strings)
        }

        val bio = player.bio
        if (!bio.isNullOrBlank()) {
            item(key = "bio") {
                Section(title = strings.about) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "stats") {
            Section(title = strings.experience) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            label = strings.matchesOrganized,
                            value = player.matchesOrganized.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = strings.matchesParticipated,
                            value = player.matchesParticipated.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            label = strings.joinRate,
                            value = strings.percentValue(player.joinRate),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = strings.cancelRate,
                            value = strings.percentValue(player.cancelRate),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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

        item(key = "reviews-title") {
            Text(
                text = strings.reviews,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        when {
            state.isLoadingRatings -> item(key = "reviews-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.previewRatings.isEmpty() -> item(key = "reviews-empty") {
                Text(
                    text = strings.noReviews,
                    style = MaterialTheme.typography.bodySmall,
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
                            Text(strings.seeAllReviews)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (!player.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = player.photoUrl,
                    contentDescription = player.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AVATAR_SIZE),
                )
            }
        }

        Text(
            text = player.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        RatingSummaryHeadline(player = player, strings = strings)

        val memberSince = formatShortDate(player.memberSince * MILLIS_PER_SECOND)
        if (memberSince.isNotEmpty()) {
            Text(
                text = strings.memberSince(memberSince),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun RatingSummaryHeadline(
    player: PlayerDetails,
    strings: PlayerDetailsStrings,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RatingStars(
            rating = player.averageRating,
            starSize = 20.dp,
            contentDescription = strings.ratingAccessibility(player.averageRating),
        )
        Text(
            text = strings.ratingValue(player.averageRating),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = strings.ratingsCount(player.totalRatings),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(STAT_CARD_HEIGHT),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val AVATAR_SIZE = 120.dp
private val STAT_CARD_HEIGHT = 80.dp
private const val MILLIS_PER_SECOND = 1_000L
