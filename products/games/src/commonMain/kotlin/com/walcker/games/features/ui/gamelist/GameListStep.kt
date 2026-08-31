package com.walcker.games.features.ui.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.features.ui.matchdetail.MatchDetailStep
import com.walcker.games.strings.GameListStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.HomeViewCoordinator
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

internal class GameListStep : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stepModel = koinScreenModel<GameListStepModel>()
        val homeViewCoordinator = koinInject<HomeViewCoordinator>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is GameListEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)

                    is GameListEffect.NavigateToMatchDetail ->
                        navigator.push(MatchDetailStep(effect.matchId))
                }
            }
        }

        GameListContent(
            state = state,
            onEvent = stepModel::onEvent,
            onShowMap = { homeViewCoordinator.setShowMap(true) },
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
internal fun GameListContent(
    state: GameListState,
    onEvent: (GameListEvents) -> Unit,
    modifier: Modifier = Modifier,
    onShowMap: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val strings = state.strings

    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CedarTokens.spacing.lg,
                        vertical = CedarTokens.spacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CedarScreenTitle(
                    title = strings.title,
                    subtitle = strings.subtitle,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShowMap) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = strings.showMapAction,
                    )
                }
            }

            if (state.preferencesLoaded) {
                FilterBar(
                    strings = strings,
                    selectedSport = state.selectedSport,
                    radiusKm = state.radiusKm,
                    onSelectSport = { onEvent(GameListEvents.SelectSport(it)) },
                    onRadiusChange = { onEvent(GameListEvents.SetRadius(it)) },
                )
            }

            when {
                state.isLoading && state.games.isEmpty() ->
                    LoadingContent(
                        contentDescription = strings.loadingLabel,
                        modifier = Modifier.fillMaxSize(),
                    )

                state.errorMessage != null && state.games.isEmpty() ->
                    EmptyState(
                        message = state.errorMessage,
                        modifier = Modifier.fillMaxSize(),
                    )

                state.games.isEmpty() ->
                    EmptyState(
                        message = strings.emptyMessage,
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> GameList(
                    strings = strings,
                    games = state.games,
                    onClick = { onEvent(GameListEvents.SelectGame(it)) },
                )
            }
        }
    }
}

@Composable
private fun FilterBar(
    strings: GameListStrings,
    selectedSport: Sport?,
    radiusKm: Double,
    onSelectSport: (Sport?) -> Unit,
    onRadiusChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = CedarTokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
            contentPadding = PaddingValues(horizontal = CedarTokens.spacing.lg),
        ) {
            item {
                SportChip(
                    label = strings.allSportsChip,
                    selected = selectedSport == null,
                    onClick = { onSelectSport(null) },
                )
            }
            items(items = Sport.entries) { sport ->
                SportChip(
                    label = sport.label,
                    selected = selectedSport == sport,
                    onClick = { onSelectSport(sport) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.radiusLabel(radiusKm.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = CedarTokens.spacing.sm),
            )
            Slider(
                value = radiusKm.toFloat(),
                onValueChange = { onRadiusChange(it.toDouble()) },
                valueRange = GameListState.MIN_RADIUS_KM.toFloat()..GameListState.MAX_RADIUS_KM.toFloat(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GameList(
    strings: GameListStrings,
    games: ImmutableList<Game>,
    onClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
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
            )
        }
    }
}

@Composable
private fun LoadingContent(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CedarLoading(contentDescription = contentDescription)
    }
}
