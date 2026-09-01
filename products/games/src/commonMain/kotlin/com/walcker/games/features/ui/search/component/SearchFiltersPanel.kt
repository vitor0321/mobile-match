package com.walcker.games.features.ui.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.strings.SearchStrings
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun SearchFiltersPanel(
    strings: SearchStrings,
    selectedSports: Set<Sport>,
    onSportToggled: (Sport?) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
    ) {
        Text(
            text = strings.filtersTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        CedarFilterSection(label = strings.filterSport) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                item {
                    SportChip(
                        label = strings.allSports,
                        selected = selectedSports.isEmpty(),
                        onClick = { onSportToggled(null) },
                    )
                }
                items(items = Sport.entries) { sport ->
                    SportChip(
                        label = sport.label,
                        selected = sport in selectedSports,
                        onClick = { onSportToggled(sport) },
                    )
                }
            }
        }

        CedarFilterRow(
            label = strings.filterDate,
            value = null,
            placeholder = strings.comingSoon,
            onClick = {},
            enabled = false,
        )

        CedarFilterRow(
            label = strings.filterPrice,
            value = null,
            placeholder = strings.comingSoon,
            onClick = {},
            enabled = false,
        )

        CedarPrimaryButton(
            text = strings.applyFilters,
            onClick = onDismiss,
        )
        CedarTextButton(
            text = strings.clearFilters,
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
