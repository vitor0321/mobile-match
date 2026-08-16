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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.walcker.games.features.domain.model.Participant
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
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
        val getGameById: GetGameByIdUseCase = koinInject()
        val observeParticipants: ObserveParticipantsUseCase = koinInject()
        val stringsHolder: GamesStringsHolder = koinInject()

        val stepModel = remember {
            MatchDetailStepModel(
                getGameById = getGameById,
                observeParticipants = observeParticipants,
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
                        MatchDetailContent(
                            match = state.match!!,
                            participants = state.participants,
                        )
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
    participants: ParticipantsSummary?,
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

        // Participants section header with live count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val confirmed = participants?.confirmedCount ?: match.confirmedPlayers
            val total = participants?.totalSlots ?: match.totalPlayers
            val open = (total - confirmed).coerceAtLeast(0)

            Text(
                text = "$confirmed/$total" + if (open > 0) " ($open open)" else " (FULL)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (open == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }

        // Live participant list (from realtime subscription)
        if (participants != null) {
            ParticipantsList(participants = participants)
        } else {
            // Fallback to the static list embedded in Game.participants
            StaticParticipantsList(participantIds = match.participants, organizerName = match.organizerName)
        }
    }
}

@Composable
private fun ParticipantsList(
    participants: ParticipantsSummary,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (participants.confirmed.isNotEmpty()) {
            item {
                Text(
                    text = "Confirmed (${participants.confirmed.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(items = participants.confirmed, key = { it.userId }) { participant ->
                ParticipantRow(participant = participant, statusLabel = "✓ Confirmed")
            }
        }

        if (participants.waitlist.isNotEmpty()) {
            item {
                Text(
                    text = "Waitlist (${participants.waitlist.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(items = participants.waitlist, key = { it.userId }) { participant ->
                val pos = participant.positionInWaitlist ?: 0
                ParticipantRow(participant = participant, statusLabel = "#$pos in queue")
            }
        }
    }
}

@Composable
private fun StaticParticipantsList(
    participantIds: List<String>,
    organizerName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        participantIds.forEachIndexed { index, userId ->
            Text(
                text = "• ${if (index == 0) organizerName else "Jogador ${index + 1}"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = participant.displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (participant.hasPaid) {
            Text(
                text = "💰",
                style = MaterialTheme.typography.labelMedium,
            )
        }
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
