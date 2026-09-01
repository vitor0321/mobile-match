package com.walcker.games.features.ui.shared.playerRatings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.walcker.games.features.ui.shared.playerDetails.component.RatingCard
import com.walcker.games.features.ui.shared.playerRatings.component.SortRow
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import org.koin.core.parameter.parametersOf

private const val PREFETCH_DISTANCE = 3

private val NextPageRowHeight = 56.dp
private val NextPageSpinnerSize = 24.dp

internal data class PlayerRatingsListStep(
    val userId: String,
    val playerName: String,
) : Screen {
    override val key: String get() = "player-ratings-$userId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel =
            koinScreenModel<PlayerRatingsListStepModel>(
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

        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisible =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -1
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
            containerColor = CedarTokens.colors.canvas,
            topBar = {
                CedarTopBar(
                    title = state.playerName.ifBlank { strings.title },
                    onBack = navigator::pop,
                    backContentDescription = strings.back,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                SortRow(
                    selected = state.sort,
                    strings = strings,
                    onSortSelected = { stepModel.onEvent(PlayerRatingsEvents.SortChanged(it)) },
                )

                when {
                    state.isLoadingFirstPage ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CedarLoading(contentDescription = strings.loadingLabel)
                        }

                    state.ratings.isEmpty() && state.errorMessage != null ->
                        EmptyState(
                            message = state.errorMessage ?: strings.errorLoading,
                            actionLabel = strings.retry,
                            onAction = { stepModel.onEvent(PlayerRatingsEvents.Retry) },
                            modifier = Modifier.fillMaxSize(),
                        )

                    state.isEmpty ->
                        EmptyState(
                            message = strings.empty,
                            modifier = Modifier.fillMaxSize(),
                        )

                    else ->
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    horizontal = CedarTokens.spacing.lg,
                                    vertical = CedarTokens.spacing.md,
                                ),
                            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                        ) {
                            items(state.ratings, key = { it.id }) { rating ->
                                RatingCard(
                                    rating = rating,
                                    ratingLabel = strings.ratingValue(rating.rating.toFloat()),
                                    ratingAccessibilityLabel =
                                        strings.ratingAccessibility(
                                            rating.rating.toFloat(),
                                        ),
                                )
                            }

                            if (state.isLoadingNextPage) {
                                item(key = "next-page-loading") {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(NextPageRowHeight),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(NextPageSpinnerSize),
                                        )
                                    }
                                }
                            } else if (state.hasMore) {
                                item(key = "next-page-button") {
                                    CedarSecondaryButton(
                                        text = strings.loadMore,
                                        onClick = {
                                            stepModel.onEvent(PlayerRatingsEvents.LoadNextPage)
                                        },
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}
