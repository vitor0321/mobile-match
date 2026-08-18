package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/**
 * Rating overview: average on the left, per-star distribution bars on the right.
 *
 * Fully stateless and string-free — every label is formatted by the caller's
 * strings layer, so this component stays locale-agnostic.
 *
 * @param averageLabel already-formatted average (e.g. `"4,8"`)
 * @param totalLabel already-formatted count (e.g. `"128 avaliações"`)
 * @param distribution counts per star, ascending: index `0` is 1 star,
 *        index `4` is 5 stars. Must have exactly [STAR_LEVELS] entries.
 * @param starCountLabel renders the row label for a star level (e.g. `5` -> `"5"`)
 */
@Composable
public fun RatingSummary(
    average: Float,
    averageLabel: String,
    totalLabel: String,
    distribution: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    starCountLabel: (Int) -> String = { it.toString() },
) {
    require(distribution.size == STAR_LEVELS) {
        "distribution must have $STAR_LEVELS entries (1..5 stars), was ${distribution.size}"
    }

    val total = distribution.sum()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = averageLabel,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            RatingStars(rating = average, starSize = 14.dp)
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Highest star level first, the way review sections are usually read.
            for (stars in STAR_LEVELS downTo 1) {
                DistributionRow(
                    label = starCountLabel(stars),
                    count = distribution[stars - 1],
                    total = total,
                )
            }
        }
    }
}

@Composable
private fun DistributionRow(
    label: String,
    count: Int,
    total: Int,
) {
    val fraction = if (total <= 0) 0f else count.toFloat() / total

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(12.dp),
            textAlign = TextAlign.End,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                ),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        ),
                )
            }
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
    }
}

/** Number of discrete star levels a rating can take (1..5). */
public const val STAR_LEVELS: Int = 5
