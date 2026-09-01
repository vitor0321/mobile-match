package com.walcker.games.features.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.ui.home.component.FilterBar
import com.walcker.games.features.ui.home.component.GameList
import com.walcker.games.features.ui.home.component.LoadingContent
import com.walcker.games.features.ui.home.map.MapBody
import com.walcker.games.features.ui.home.map.MapStepModel
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import com.walcker.match.navigator.HomeViewCoordinator
import com.walcker.match.navigator.MatchDetailCoordinator
import org.koin.compose.koinInject

internal class GameListStep : Screen {
    @Composable
    override fun Content() {
        val homeViewCoordinator = koinInject<HomeViewCoordinator>()
        val showMap by homeViewCoordinator.showMap.collectAsState()
        val stepModel = koinScreenModel<GameListStepModel>()
        val matchDetailCoordinator = koinInject<MatchDetailCoordinator>()
        val state by stepModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            stepModel.effects.collect { effect ->
                when (effect) {
                    is GameListEffect.ShowMessage ->
                        snackbarHostState.showSnackbar(effect.message)

                    is GameListEffect.NavigateToMatchDetail ->
                        matchDetailCoordinator.open(effect.matchId)
                }
            }
        }

        GameListContent(
            state = state,
            onEvent = stepModel::onEvent,
            showMap = showMap,
            onToggleMap = { homeViewCoordinator.setShowMap(!showMap) },
            snackbarHostState = snackbarHostState,
            mapBody = { bodyModifier ->
                val mapStepModel = koinScreenModel<MapStepModel>()
                val deepLinkCoordinator = koinInject<DeepLinkCoordinator>()
                val mapState by mapStepModel.state.collectAsState()

                MapBody(
                    state = mapState,
                    onRefresh = mapStepModel::onRefresh,
                    onRetryLocation = mapStepModel::onRetryLocation,
                    onPinClick = { matchId -> deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId)) },
                    modifier = bodyModifier,
                )
            },
        )
    }
}

@Composable
internal fun GameListContent(
    state: GameListState,
    onEvent: (GameListEvents) -> Unit,
    modifier: Modifier = Modifier,
    showMap: Boolean = false,
    onToggleMap: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    mapBody: @Composable (Modifier) -> Unit = {},
) {
    val strings = state.strings

    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Row(
                modifier =
                    Modifier
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
                IconButton(onClick = onToggleMap) {
                    Icon(
                        imageVector = if (showMap) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                        contentDescription = if (showMap) strings.showListAction else strings.showMapAction,
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
                showMap -> mapBody(Modifier.fillMaxSize())

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

                else ->
                    GameList(
                        strings = strings,
                        games = state.games,
                        onClick = { onEvent(GameListEvents.SelectGame(it)) },
                    )
            }
        }
    }
}
