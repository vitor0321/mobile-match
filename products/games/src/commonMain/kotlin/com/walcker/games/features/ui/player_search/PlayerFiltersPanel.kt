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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.PlayerSearchStrings

/**
 * Bottom sheet with the advanced player search filters.
 *
 * Only rating and sport: the match-count filters were removed in Sprint 3
 * because nothing writes those counters, so they silently excluded everyone.
 */
@Composable
internal fun PlayerFiltersPanel(
    filters: PlayerSearchFilters,
    strings: PlayerSearchStrings,
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
            text = strings.filtersTitle,
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = strings.ratingSection,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RatingBoundField(
                value = filters.minRating,
                label = strings.ratingMin,
                onValueChange = { onFiltersChanged(filters.copy(minRating = it)) },
                modifier = Modifier.weight(1f),
            )
            RatingBoundField(
                value = filters.maxRating,
                label = strings.ratingMax,
                onValueChange = { onFiltersChanged(filters.copy(maxRating = it)) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = strings.sportsSection,
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sport.entries.forEach { sport ->
                val selected = sport in filters.favoriteSports
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updated = if (selected) {
                            filters.favoriteSports - sport
                        } else {
                            filters.favoriteSports + sport
                        }
                        onFiltersChanged(filters.copy(favoriteSports = updated))
                    },
                    label = { Text(sport.label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

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
                Text(strings.clearFilters)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.applyFilters)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Rating bound input.
 *
 * Anything outside `0..5` is clamped and an unparseable value clears the bound
 * instead of freezing the field — the filter is a hint, not a form to validate.
 */
@Composable
private fun RatingBoundField(
    value: Float?,
    label: String,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { text ->
            onValueChange(text.toFloatOrNull()?.coerceIn(0f, MAX_RATING))
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private const val MAX_RATING = 5f
