package com.walcker.games.features.ui.playerprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.core.format.formatDecimal

internal class PlayerProfileStep : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val strings = rememberGamesStrings().strings.playerProfile
        val model = koinScreenModel<PlayerProfileStepModel>()
        val state by model.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissError)
            }
        }

        LaunchedEffect(state.availabilityErrorMessage) {
            state.availabilityErrorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissAvailabilityError)
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
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    return@Scaffold
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp,
                    ),
                ) {
                    // User info card
                    item {
                        state.userName?.let { name ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    state.userEmail?.let { email ->
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Disponibilidade (regra B5) — logo abaixo da identificação
                    // porque é o único controle da tela que muda o que a pessoa
                    // recebe, e não só o que ela vê.
                    item {
                        AvailabilityCard(
                            isAvailable = state.isAvailable,
                            isUpdating = state.isUpdatingAvailability,
                            strings = strings,
                            onCheckedChange = { checked ->
                                model.onEvent(PlayerProfileEvent.AvailabilityChanged(checked))
                            },
                        )
                    }

                    // Stats grid
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                label = strings.statsOrganized,
                                value = state.matchesOrganized.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                label = strings.statsParticipated,
                                value = state.matchesParticipated.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Ratings summary and history
                    if (state.totalRatings > 0) {
                        item {
                            RatingsSummaryCard(
                                averageRating = state.averageRating,
                                totalRatings = state.totalRatings,
                            )
                        }

                        item {
                            Text(
                                text = "Avaliações Recebidas",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        items(items = state.ratings, key = { it.id }) { rating ->
                            RatingItemCard(rating = rating)
                        }
                    }
                }

                // Settings section with logout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Configurações",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    OutlinedButton(
                        onClick = { model.onEvent(PlayerProfileEvent.LogoutRequested) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Fazer logout")
                    }
                }
            }
        }
    }
}

/**
 * O switch é otimista — acompanha o dedo e o StepModel reverte se a gravação
 * falhar — mas fica travado enquanto a escrita está em voo, para um toque
 * repetido não virar uma fila de escritas concorrentes no mesmo documento.
 */
@Composable
private fun AvailabilityCard(
    isAvailable: Boolean,
    isUpdating: Boolean,
    strings: PlayerProfileStrings,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.availabilityTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    // Descreve a consequência, não o estado: "desligado" não
                    // diz a ninguém que vai parar de receber aviso de partida.
                    text = if (isAvailable) {
                        strings.availabilityOnDescription
                    } else {
                        strings.availabilityOffDescription
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isAvailable,
                onCheckedChange = onCheckedChange,
                enabled = !isUpdating,
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingsSummaryCard(
    averageRating: Float,
    totalRatings: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Avaliação Geral",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = formatDecimal(value = averageRating, decimals = 1),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "⭐ ".repeat(averageRating.toInt()) + "☆".repeat(5 - averageRating.toInt()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "$totalRatings avaliações",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingItemCard(
    rating: Rating,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⭐ ".repeat(rating.rating) + "☆".repeat(5 - rating.rating),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatRatingDate(rating.createdAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rating.comment.isNotEmpty()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Formats a timestamp as a date (e.g., "12 dias atrás").
 */
private fun formatRatingDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = (now - timestamp) / (1000 * 60 * 60 * 24)
    return when {
        diffDays < 1 -> "Hoje"
        diffDays < 7 -> "$diffDays dias atrás"
        diffDays < 30 -> "${diffDays / 7} semanas atrás"
        else -> "${diffDays / 30} meses atrás"
    }
}
