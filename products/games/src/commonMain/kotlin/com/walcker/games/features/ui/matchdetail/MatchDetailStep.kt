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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.model.Participant
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.ui.ratings.RatingBottomSheet
import com.walcker.games.features.domain.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.usecase.ObserveParticipantsUseCase
import com.walcker.games.features.domain.usecase.SubmitRatingUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.identity.api.SessionHolder
import com.walcker.match.navigator.PromotionCoordinator
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
        val observeMatch: ObserveMatchUseCase = koinInject()
        val observeParticipants: ObserveParticipantsUseCase = koinInject()
        val submitRating: SubmitRatingUseCase = koinInject()
        val sessionHolder: SessionHolder = koinInject()
        val promotionCoordinator: PromotionCoordinator = koinInject()
        val stringsHolder: GamesStringsHolder = koinInject()

        val joinGame: com.walcker.games.features.domain.usecase.JoinGameUseCase = koinInject()
        val leaveGame: com.walcker.games.features.domain.usecase.LeaveMatchUseCase = koinInject()
        val cancelGame: com.walcker.games.features.domain.usecase.CancelMatchUseCase = koinInject()

        val stepModel = remember {
            MatchDetailStepModel(
                getGameById = getGameById,
                observeMatch = observeMatch,
                observeParticipants = observeParticipants,
                joinGame = joinGame,
                leaveMatch = leaveGame,
                cancelMatch = cancelGame,
                submitRating = submitRating,
                sessionHolder = sessionHolder,
                promotionCoordinator = promotionCoordinator,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Promotion banner — shown when the user just moved from waitlist to confirmed.
                if (state.justPromoted) {
                    PromotionBanner(
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissPromotion) },
                    )
                }

                // Success message banner — shown after successfully joining a match.
                state.successMessage?.let { message ->
                    SuccessBanner(
                        message = message,
                        onDismiss = { stepModel.onEvent(MatchDetailEvent.DismissSuccess) },
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
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
                                canRate = state.match!!.status == MatchStatus.FINISHED,
                                isJoining = state.isJoining,
                                isLeavingMatch = state.isLeavingMatch,
                                isCancellingMatch = state.isCancellingMatch,
                                onRatePlayer = { userId, displayName ->
                                    stepModel.onEvent(
                                        MatchDetailEvent.OpenRatingSheet(userId, displayName),
                                    )
                                },
                                onJoinMatch = {
                                    stepModel.onEvent(MatchDetailEvent.JoinMatch)
                                },
                                onLeaveMatch = {
                                    stepModel.onEvent(MatchDetailEvent.RequestLeaveMatch)
                                },
                                onCancelMatch = {
                                    stepModel.onEvent(MatchDetailEvent.RequestCancelMatch)
                                },
                            )
                        }

                        else -> {
                            EmptyContent()
                        }
                    }
                }
            }
        }

        // Rating bottom sheet — shown when user taps "Rate" on a participant.
        RatingBottomSheet(
            isVisible = state.showRatingSheet,
            playerName = state.selectedPlayerForRating?.second ?: "",
            onDismiss = { stepModel.onEvent(MatchDetailEvent.CloseRatingSheet) },
            onSubmit = { rating, comment ->
                stepModel.onEvent(MatchDetailEvent.SubmitRating(rating, comment))
            },
            isLoading = state.isSubmittingRating,
        )

        // Leave match confirmation dialog
        if (state.showLeaveConfirmDialog) {
            AlertDialog(
                onDismissRequest = { stepModel.onEvent(MatchDetailEvent.CancelLeaveMatch) },
                title = { Text("Sair da Partida?") },
                text = {
                    Text("Tem certeza de que deseja sair desta partida? Sua vaga será liberada para o próximo da fila.")
                },
                confirmButton = {
                    Button(
                        onClick = { stepModel.onEvent(MatchDetailEvent.ConfirmLeaveMatch) },
                        enabled = !state.isLeavingMatch,
                    ) {
                        if (state.isLeavingMatch) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Sair")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { stepModel.onEvent(MatchDetailEvent.CancelLeaveMatch) },
                    ) {
                        Text("Cancelar")
                    }
                },
            )
        }

        // Cancel match confirmation dialog
        if (state.showCancelConfirmDialog) {
            AlertDialog(
                onDismissRequest = { stepModel.onEvent(MatchDetailEvent.CancelCancelMatch) },
                title = { Text("Cancelar Partida?") },
                text = {
                    Text("Tem certeza de que deseja cancelar esta partida? Todos os participantes serão notificados.")
                },
                confirmButton = {
                    Button(
                        onClick = { stepModel.onEvent(MatchDetailEvent.ConfirmCancelMatch) },
                        enabled = !state.isCancellingMatch,
                    ) {
                        if (state.isCancellingMatch) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Cancelar Partida")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { stepModel.onEvent(MatchDetailEvent.CancelCancelMatch) },
                    ) {
                        Text("Voltar")
                    }
                },
            )
        }
    }
}

@Composable
private fun PromotionBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Você foi promovido da fila!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("✕", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SuccessBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("✕", color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun MatchDetailContent(
    match: com.walcker.games.features.domain.model.Game,
    participants: ParticipantsSummary?,
    canRate: Boolean,
    isJoining: Boolean,
    isLeavingMatch: Boolean,
    isCancellingMatch: Boolean,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    onJoinMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onCancelMatch: () -> Unit,
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

        // Live status badge (from realtime match subscription)
        StatusBadge(status = match.status)

        Spacer(modifier = Modifier.height(8.dp))

        // Match details
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            ParticipantsList(
                participants = participants,
                canRate = canRate,
                onRatePlayer = onRatePlayer,
            )
        } else {
            // Fallback to the static list embedded in Game.participants
            StaticParticipantsList(participantIds = match.participants, organizerName = match.organizerName)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Join button — reacts live to status and slot availability.
        val confirmed = participants?.confirmedCount ?: match.confirmedPlayers
        val total = participants?.totalSlots ?: match.totalPlayers
        val isFull = confirmed >= total || match.status == MatchStatus.FULL
        val isClosed = match.status == MatchStatus.FINISHED || match.status == MatchStatus.CANCELLED

        JoinButton(
            enabled = !isFull && !isClosed && !isJoining,
            isLoading = isJoining,
            label = when {
                isClosed -> "Partida encerrada"
                isFull -> "Entrar na fila de espera"
                else -> "Entrar na partida"
            },
            onClick = onJoinMatch,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons: Leave / Cancel (shown conditionally based on user role)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Leave match button (participant)
            Button(
                onClick = onLeaveMatch,
                enabled = !isClosed && !isLeavingMatch && !isCancellingMatch,
                modifier = Modifier.weight(1f),
            ) {
                if (isLeavingMatch) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Sair")
                }
            }

            // Cancel match button (organizer only)
            Button(
                onClick = onCancelMatch,
                enabled = !isClosed && !isCancellingMatch && !isLeavingMatch,
                modifier = Modifier.weight(1f),
            ) {
                if (isCancellingMatch) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: MatchStatus,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (status) {
        MatchStatus.OPEN -> "Aberta" to MaterialTheme.colorScheme.primary
        MatchStatus.FULL -> "Lotada" to MaterialTheme.colorScheme.error
        MatchStatus.FINISHED -> "Encerrada" to MaterialTheme.colorScheme.onSurfaceVariant
        MatchStatus.CANCELLED -> "Cancelada" to MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun JoinButton(
    enabled: Boolean,
    isLoading: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text = label)
        }
    }
}

@Composable
private fun ParticipantsList(
    participants: ParticipantsSummary,
    canRate: Boolean,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
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
                ParticipantRow(
                    participant = participant,
                    statusLabel = "✓ Confirmed",
                    canRate = canRate,
                    onRatePlayer = onRatePlayer,
                )
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
                ParticipantRow(
                    participant = participant,
                    statusLabel = "#$pos in queue",
                    canRate = false, // Only rate confirmed players
                    onRatePlayer = onRatePlayer,
                )
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
    canRate: Boolean,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
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

        // Rate button — only shown after match is finished.
        if (canRate) {
            TextButton(
                onClick = { onRatePlayer(participant.userId, participant.displayName) },
            ) {
                Text("⭐ Avaliar")
            }
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
