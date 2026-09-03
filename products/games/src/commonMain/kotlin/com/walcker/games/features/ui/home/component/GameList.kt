package com.walcker.games.features.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.strings.GameListStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.collections.immutable.ImmutableList

private const val PREFETCH_DISTANCE = 3

private val NextPageRowHeight = 56.dp
private val NextPageSpinnerSize = 24.dp

@Composable
internal fun GameList(
    strings: GameListStrings,
    games: ImmutableList<Game>,
    onClick: (String) -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val listState = rememberLazyListState()

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

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }.collect { reachedEnd ->
            if (reachedEnd) onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.xs,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        items(items = games, key = { it.id }) { game ->
            MatchCard(
                venueName = game.venueName,
                startsAtSeconds = game.startsAtSeconds,
                metaLabel = "${game.sport.label} · ${game.neighborhood}",
                priceLabel = game.pricePerPlayer?.let { strings.perPlayer(it) },
                slotsLabel = strings.slotsBadge(game.openSlots),
                openSlots = game.openSlots,
                onClick = { onClick(game.id) },
                matchRating = game.matchRating.toFloat().takeIf { game.matchRatingCount > 0 },
                matchRatingCountLabel =
                    strings.ratingsCount(game.matchRatingCount).takeIf { game.matchRatingCount > 0 },
            )
        }

        if (isLoadingMore) {
            item(key = "next-page-loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(NextPageRowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    CedarLoading(
                        contentDescription = strings.loadingLabel,
                        size = NextPageSpinnerSize,
                    )
                }
            }
        } else if (hasMore) {
            item(key = "next-page-button") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CedarSecondaryButton(
                        text = strings.loadMore,
                        onClick = onLoadMore,
                        fillWidth = false,
                    )
                }
            }
        }
    }
}
