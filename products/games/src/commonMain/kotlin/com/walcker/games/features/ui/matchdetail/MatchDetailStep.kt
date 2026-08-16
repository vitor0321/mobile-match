package com.walcker.games.features.ui.matchdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.rememberGamesStrings
import org.koin.compose.koinInject

/**
 * Screen for displaying match/game details.
 * Receives matchId as a parameter.
 */
internal class MatchDetailStep(val matchId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val gameRepository: GameRepository = koinInject()
        val stringsHolder: GamesStringsHolder = koinInject()

        val stepModel = remember {
            MatchDetailStepModel(
                gameRepository = gameRepository,
                stringsHolder = stringsHolder,
                matchId = matchId,
            )
        }

        val state by stepModel.state.collectAsState()
        val strings = rememberGamesStrings().strings

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Match Details") },
                    navigationIcon = {
                        TextButton(onClick = { /* Back button handled by Navigator */ }) {
                            Text("← Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    state.errorMessage != null -> {
                        ErrorContent(
                            error = state.errorMessage ?: "Unknown error",
                            onRetry = { stepModel.onEvent(MatchDetailEvent.Retry) },
                        )
                    }

                    state.match != null -> {
                        MatchDetailContent(match = state.match!!)
                    }

                    else -> {
                        EmptyContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchDetailContent(
    match: com.walcker.games.features.domain.model.Game,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Match sport and venue
        Text(
            text = match.sport.label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = match.venueName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "${match.neighborhood}, ${match.city}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Organizer
        Text(
            text = "Organizer: ${match.organizerName} (${match.organizerRating}★)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Match details
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = match.status.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${match.durationMin} min",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column {
                Text(
                    text = "Price",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = match.pricePerPlayer ?: "Free",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Participants
        Text(
            text = "Participants",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "${match.confirmedPlayers}/${match.totalPlayers} (${match.openSlots} open)",
            style = MaterialTheme.typography.bodyMedium,
        )

        // TODO Phase 3-ETAPA3: Add more details (location, status, join button, etc.)
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Match not found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
