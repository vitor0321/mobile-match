package com.walcker.games.features.ui.mymatches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.walcker.games.strings.rememberGamesStrings

internal class MyMatchesStep : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
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
            topBar = {
                TopAppBar(title = { Text(strings.title) })
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                PrimaryTabRow(
                    selectedTabIndex = if (state.activeTab == MyMatchesTab.ACTIVE) 0 else 1,
                ) {
                    Tab(
                        selected = state.activeTab == MyMatchesTab.ACTIVE,
                        onClick = { model.onEvent(MyMatchesEvent.TabSelected(MyMatchesTab.ACTIVE)) },
                        text = { Text(strings.activeTab) },
                    )
                    Tab(
                        selected = state.activeTab == MyMatchesTab.PAST,
                        onClick = { model.onEvent(MyMatchesEvent.TabSelected(MyMatchesTab.PAST)) },
                        text = { Text(strings.pastTab) },
                    )
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    return@Scaffold
                }

                val matches = if (state.activeTab == MyMatchesTab.ACTIVE) state.active else state.past
                if (matches.isEmpty()) {
                    EmptyState(
                        title = if (state.activeTab == MyMatchesTab.ACTIVE) strings.emptyActive else strings.emptyPast,
                        subtitle = if (state.activeTab == MyMatchesTab.ACTIVE) strings.emptyActiveSubtitle else null,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                isPast = state.activeTab == MyMatchesTab.PAST,
                                onActionClick = {
                                    if (myMatch.role == com.walcker.games.features.domain.model.MatchRole.ORGANIZER) {
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

@Composable
private fun EmptyState(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
