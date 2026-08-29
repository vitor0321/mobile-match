package com.walcker.games.features.ui.player_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.DimensionAverage
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.games.strings.RatingStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_STARS = 5f

private val BarHeight = 6.dp

@Composable
internal fun DimensionAveragesCard(
    averages: Map<RatingDimension, DimensionAverage>,
    strings: PlayerDetailsStrings,
    ratingStrings: RatingStrings,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CedarTokens.radius.mdShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            CedarSectionHeader(title = strings.dimensionsTitle)

            RatingDimension.entries.forEach { dimension ->
                val average = averages[dimension] ?: return@forEach
                DimensionRow(
                    label = dimension.label(ratingStrings),
                    average = average,
                    strings = strings,
                )
            }
        }
    }
}

@Composable
private fun DimensionRow(
    label: String,
    average: DimensionAverage,
    strings: PlayerDetailsStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label: ${strings.ratingAccessibility(average.average)}"
            },
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = strings.ratingValue(average.average),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LinearProgressIndicator(
            progress = { (average.average / MAX_STARS).coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight),
        )

        Text(
            text = strings.dimensionCount(average.count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun RatingDimension.label(strings: RatingStrings): String = when (this) {
    RatingDimension.PUNCTUALITY -> strings.dimensionPunctuality
    RatingDimension.RESPECT -> strings.dimensionRespect
    RatingDimension.FAIR_PLAY -> strings.dimensionFairPlay
    RatingDimension.BEHAVIOR -> strings.dimensionBehavior
}
