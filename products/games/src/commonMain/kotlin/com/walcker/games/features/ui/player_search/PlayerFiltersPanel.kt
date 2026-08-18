package com.walcker.games.features.ui.player_search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.Sport

/**
 * ModalBottomSheet panel for advanced player search filters.
 *
 * Allows filtering by: rating range, favorite sports, min matches.
 */
@Composable
internal fun PlayerFiltersPanel(
    filters: PlayerSearchFilters,
    onFiltersChanged: (filters: PlayerSearchFilters) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Filtros Avançados",
            style = MaterialTheme.typography.headlineSmall,
        )

        // Rating range section
        Text(
            text = "Avaliação",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = filters.minRating?.toString() ?: "",
                onValueChange = { value ->
                    val minRating = value.toFloatOrNull()
                    onFiltersChanged(filters.copy(minRating = minRating))
                },
                modifier = Modifier.weight(1f),
                label = { Text("Mínimo") },
                placeholder = { Text("0.0") },
                singleLine = true,
            )
            OutlinedTextField(
                value = filters.maxRating?.toString() ?: "",
                onValueChange = { value ->
                    val maxRating = value.toFloatOrNull()
                    onFiltersChanged(filters.copy(maxRating = maxRating))
                },
                modifier = Modifier.weight(1f),
                label = { Text("Máximo") },
                placeholder = { Text("5.0") },
                singleLine = true,
            )
        }

        // Sports filter section
        Text(
            text = "Esportes Favoritos",
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sport.entries.forEach { sport ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = filters.favoriteSports.contains(sport),
                        onCheckedChange = { isChecked ->
                            val newSports = if (isChecked) {
                                filters.favoriteSports + sport
                            } else {
                                filters.favoriteSports - sport
                            }
                            onFiltersChanged(filters.copy(favoriteSports = newSports))
                        },
                    )
                    Text(
                        text = sport.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Min matches section
        Text(
            text = "Experiência Mínima",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = filters.minMatchesOrganized?.toString() ?: "",
                onValueChange = { value ->
                    val minMatches = value.toIntOrNull()
                    onFiltersChanged(filters.copy(minMatchesOrganized = minMatches))
                },
                modifier = Modifier.weight(1f),
                label = { Text("Partidas Organizadas") },
                placeholder = { Text("0") },
                singleLine = true,
            )
            OutlinedTextField(
                value = filters.minMatchesParticipated?.toString() ?: "",
                onValueChange = { value ->
                    val minMatches = value.toIntOrNull()
                    onFiltersChanged(filters.copy(minMatchesParticipated = minMatches))
                },
                modifier = Modifier.weight(1f),
                label = { Text("Partidas Participadas") },
                placeholder = { Text("0") },
                singleLine = true,
            )
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onResetFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text("Limpar Filtros")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Aplicar")
            }
        }

        // Spacer for bottom navigation
        Spacer(modifier = Modifier.height(16.dp))
    }
}
