package com.walcker.games.features.ui.shared.playerSearch.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.strings.PlayerSearchStrings
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_RATING = 5f

private fun String.toRatingOrNull(): Float? = replace(',', '.').toFloatOrNull()?.coerceIn(0f, MAX_RATING)

@OptIn(ExperimentalLayoutApi::class)
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        CedarScreenTitle(title = strings.filtersTitle)

        CedarFilterSection(label = strings.ratingSection) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
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
        }

        CedarFilterSection(label = strings.sportsSection) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Sport.entries.forEach { sport ->
                    val selected = sport in filters.favoriteSports
                    SportChip(
                        label = sport.label,
                        selected = selected,
                        onClick = {
                            val updated =
                                if (selected) {
                                    filters.favoriteSports - sport
                                } else {
                                    filters.favoriteSports + sport
                                }
                            onFiltersChanged(filters.copy(favoriteSports = updated))
                        },
                    )
                }
            }
        }

        CedarPrimaryButton(
            text = strings.applyFilters,
            onClick = onDismiss,
            modifier = Modifier.padding(top = CedarTokens.spacing.xs),
        )
        CedarTextButton(
            text = strings.clearFilters,
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(CedarTokens.spacing.xl))
    }
}

@Composable
private fun RatingBoundField(
    value: Float?,
    label: String,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value?.toString().orEmpty()) }

    LaunchedEffect(value) {
        if (text.toRatingOrNull() != value) {
            text = value?.toString().orEmpty()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            text = typed
            onValueChange(typed.toRatingOrNull())
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = CedarTokens.radius.smShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
