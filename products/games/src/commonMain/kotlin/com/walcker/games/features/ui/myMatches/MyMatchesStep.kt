package com.walcker.games.features.ui.myMatches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.ui.myMatches.component.MyMatchCard
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.EmptyState
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.TabCoordinator
import org.koin.compose.koinInject

internal class MyMatchesStep : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val tabCoordinator = koinInject<TabCoordinator>()
        val strings = rememberGamesStrings().strings.myMatches
        val model = koinScreenModel<MyMatchesStepModel>()
        val state by model.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(MyMatchesEvent.DismissError)
            }
        }

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                CedarScreenTitle(
                    title = strings.title,
                    modifier =
                        Modifier.padding(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.md,
                        ),
                )

                val isActiveTab = state.activeTab == MyMatchesTab.ACTIVE
                PrimaryTabRow(selectedTabIndex = if (isActiveTab) 0 else 1) {
                    Tab(
                        selected = isActiveTab,
                        onClick = { model.onEvent(MyMatchesEvent.TabSelected(MyMatchesTab.ACTIVE)) },
                        text = { Text(strings.activeTab) },
                    )
                    Tab(
                        selected = !isActiveTab,
                        onClick = { model.onEvent(MyMatchesEvent.TabSelected(MyMatchesTab.PAST)) },
                        text = { Text(strings.pastTab) },
                    )
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CedarLoading(contentDescription = strings.loadingLabel)
                    }
                    return@Scaffold
                }

                val matches = if (isActiveTab) state.active else state.past
                if (matches.isEmpty()) {
                    EmptyState(
                        message = if (isActiveTab) strings.emptyActive else strings.emptyPast,
                        supportingText = if (isActiveTab) strings.emptyActiveSubtitle else null,
                        actionLabel = if (isActiveTab) strings.emptyActiveAction else null,
                        onAction =
                            if (isActiveTab) {
                                { tabCoordinator.requestTab(MainTab.Search) }
                            } else {
                                null
                            },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                horizontal = CedarTokens.spacing.lg,
                                vertical = CedarTokens.spacing.md,
                            ),
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                    ) {
                        items(matches, key = { it.game.id }) { myMatch ->
                            MyMatchCard(
                                myMatch = myMatch,
                                organizerBadge = strings.organizerBadge,
                                participantBadge = strings.participantBadge,
                                cancelActionLabel = strings.cancelAction,
                                leaveActionLabel = strings.leaveAction,
                                statusCancelledLabel = strings.statusCancelled,
                                statusFinishedLabel = strings.statusFinished,
                                playersLabel =
                                    strings.playersCount(
                                        myMatch.game.confirmedPlayers,
                                        myMatch.game.totalPlayers,
                                    ),
                                isPast = !isActiveTab,
                                onActionClick = {
                                    if (myMatch.role == MatchRole.ORGANIZER) {
                                        model.onEvent(MyMatchesEvent.CancelRequested(myMatch.game.id))
                                    } else {
                                        model.onEvent(MyMatchesEvent.LeaveRequested(myMatch.game.id))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
