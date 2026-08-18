package com.walcker.games.features.ui.player_ratings

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.ui.player_details.RatingCard
import com.walcker.games.strings.PlayerRatingsStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.EmptyState
import org.koin.core.parameter.parametersOf

/**
 * Full list of the reviews a player received: 20 per page, sorted server-side.
 *
 * Reached from the player details screen; the player's name is passed in so the
 * title renders immediately instead of waiting on a second profile read.
 */
internal data class PlayerRatingsListStep(
    val userId: String,
    val playerName: String,
) : Screen {

    override val key: String get() = "player-ratings-$userId"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<PlayerRatingsListStepModel>(
            parameters = { parametersOf(userId, playerName) },
        )
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val strings = rememberGamesStrings().strings.playerRatings
        val listState = rememberLazyListState()

        LaunchedEffect(stepModel) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is PlayerRatingsEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        // Prefetch the next page slightly before the user reaches the bottom.
        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && lastVisible >= total - PREFETCH_DISTANCE
            }
        }

        LaunchedEffect(listState, stepModel) {
            snapshotFlow { shouldLoadMore }.collect { reachedEnd ->
                if (reachedEnd) {
                    stepModel.onEvent(PlayerRatingsEvents.LoadNextPage)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.playerName.ifBlank { strings.title },
                        )
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SortRow(
                    selected = state.sort,
                    strings = strings,
                    onSortSelected = { stepModel.onEvent(PlayerRatingsEvents.SortChanged(it)) },
                )

                when {
                    state.isLoadingFirstPage -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    state.ratings.isEmpty() && state.errorMessage != null -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.errorMessage ?: strings.errorLoading,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = { stepModel.onEvent(PlayerRatingsEvents.Retry) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(strings.retry)
                        }
                    }

                    state.isEmpty -> EmptyState(
                        message = strings.empty,
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.ratings, key = { it.id }) { rating ->
                            RatingCard(
                                rating = rating,
                                ratingLabel = strings.ratingValue(rating.rating.toFloat()),
                                ratingAccessibilityLabel = strings.ratingAccessibility(
                                    rating.rating.toFloat(),
                                ),
                            )
                        }

                        if (state.isLoadingNextPage) {
                            item(key = "next-page-loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        } else if (state.hasMore) {
                            // Manual fallback for when auto-prefetch cannot fire
                            // (short lists, accessibility navigation).
                            item(key = "next-page-button") {
                                Button(
                                    onClick = {
                                        stepModel.onEvent(PlayerRatingsEvents.LoadNextPage)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(strings.loadMore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    selected: RatingSort,
    strings: PlayerRatingsStrings,
    onSortSelected: (RatingSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSortSelected(sort) },
                label = { Text(strings.labelFor(sort)) },
            )
        }
    }
}

private fun PlayerRatingsStrings.labelFor(sort: RatingSort): String = when (sort) {
    RatingSort.RECENT -> sortRecent
    RatingSort.HIGHEST -> sortHighest
    RatingSort.LOWEST -> sortLowest
}

/** How many items before the end trigger the next page request. */
private const val PREFETCH_DISTANCE = 3
